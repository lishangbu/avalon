package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.battle.application.query.BattleQueryService;
import io.github.lishangbu.avalon.battle.domain.BattleState.Order;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.world.SceneActionCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.creature.application.command.CreatureEvolutionCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureEvolutionPreviewService;
import io.github.lishangbu.avalon.creature.application.command.EvolutionTeachingCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.CultivationMaterialService.Mode;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.command.SceneActionCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import static org.junit.jupiter.api.Assertions.*;

/** 从注册连续完成74项任务；不写数据库夹具、不赠经验、不修改战斗种子。 */
@Import(FormalChapterJourney.class)
class NaturalExtendedStoryTest extends SpringIntegrationTest {

	@Autowired
	FormalChapterJourney opening;

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService queries;

	@Autowired
	CharacterFactQueryService facts;

	@Autowired
	QuestCatalog quests;

	@Autowired
	QuestCommandService questCommands;

	@Autowired
	QuestAcceptCommandService acceptance;

	@Autowired
	SceneActionCatalog scenes;

	@Autowired
	SceneActionCommandService sceneCommands;

	@Autowired
	BattleCatalog battles;

	@Autowired
	BattleCommandService battleCommands;

	@Autowired
	BattleQueryService battleQueries;

	@Autowired
	WorldCatalog world;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	CreatureQueryService creatureQueries;

	@Autowired
	EvolutionTeachingCommandService evolutionTeaching;

	@Autowired
	CreatureEvolutionPreviewService evolutionPreview;

	@Autowired
	CreatureEvolutionCommandService evolution;

	@ParameterizedTest
	@CsvSource({ "WARRIOR,1", "MAGE,1", "PRIEST,1", "RANGER,1", "WARRIOR,5", "MAGE,5", "PRIEST,5", "RANGER,5" })
	void openingToPortUsesOnlyEarnedResources(String profession, int recoveryInterval) {
		String name = "journey_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Journey-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, profession);
		opening.complete(owner);
		for (int number = 31; number <= 74; number++) {
			equipEarnedGear(owner);
			var step = quests.step("BQ%03d".formatted(number));
			// 每条新任务先走到发布者身边接取，再执行调查；后续步骤仍沿用整项任务的参与状态。
			if (List.of(31, 40, 49, 57, 67).contains(number)) {
				walk(owner, step.acceptance().location());
				acceptance.accept(owner, step.id(),
						new QuestAcceptRequest(UUID.randomUUID().toString(), queries.find(owner).revision()));
			}
			for (String fact : step.facts())
				satisfy(owner, fact);
			walk(owner, step.location());
			questCommands.interact(owner, step.id());
			assertTrue(facts.codes(owner).contains("QUEST:" + step.id()));
			System.out.printf("NATURAL_EXTENDED profession=%s step=%s level=%d xp=%d%n", profession, step.id(),
					queries.find(owner).level(), queries.find(owner).experience());
		}
		assertEquals("UNDINE_CARE_STATION", queries.find(owner).location());
		// 每一步都必须留下正式完成事实，不能只凭最终地点推断整条故事已经完成。
		var completed = facts.codes(owner);
		for (int number = 1; number <= 74; number++)
			assertTrue(completed.contains("QUEST:BQ%03d".formatted(number)));
		// 最终交接的重放不能再次发放经验或金币。
		var beforeReplay = queries.find(owner);
		questCommands.interact(owner, "BQ074");
		assertEquals(beforeReplay.experience(), queries.find(owner).experience());
		assertEquals(beforeReplay.gold(), queries.find(owner).gold());
		// 团聚后在正式浅滩野战补足等级缺口，保留完整任务路线获得的装备与伙伴。
		int trainingFights = 0;
		int streak = 0, longestStreak = 0, recoveries = 0;
		while (queries.find(owner).level() < 16 && trainingFights < 100) {
			equipEarnedGear(owner);
			long before = queries.find(owner).experience();
			var previous = battleQueries.current(owner);
			boolean lowResources = previous != null && previous.units()
				.stream()
				.filter(unit -> !unit.enemy() && (unit.hero() || unit.position() < 2))
				.anyMatch(unit -> unit.hp() * 10 < unit.stats().health() * 7 || unit.mp() * 4 < unit.stats().mana());
			boolean recover = streak == 0 || streak >= recoveryInterval || lowResources;
			if (recover) {
				streak = 0;
				recoveries++;
			}
			fight(owner, "SHALLOWS_WILD", recover);
			longestStreak = Math.max(longestStreak, ++streak);
			assertTrue(queries.find(owner).experience() > before, "正常野战必须实际获得经验");
			trainingFights++;
		}
		assertEquals(16, queries.find(owner).level(), "主线后的等级缺口应能通过正常浅滩探索补足");
		System.out.printf(
				"NATURAL_EXTENDED_TRAINING profession=%s recoveryInterval=%d fights=%d longestStreak=%d recoveries=%d xp=%d%n",
				profession, recoveryInterval, trainingFights, longestStreak, recoveries,
				queries.find(owner).experience());
		if (profession.equals("WARRIOR") && recoveryInterval == 5)
			evolveWithWildMaterials(owner);
		walk(owner, "UNDINE_WHARF");
		battleCommands.recover(owner);
	}

	/** 教学选其他家族，目标路线仅使用实际野战掉落，避免把免费材料当作自然收集。 */
	private void evolveWithWildMaterials(long owner) {
		walk(owner, "CITY_PETS");
		evolutionTeaching.learn(owner, new EvolutionTeachingCommandService.Request("STONE_GOLEM", true));
		var cat = creatureQueries.list(owner)
			.stream()
			.filter(pet -> pet.species().equals("CLAW_CAT"))
			.findFirst()
			.orElseThrow();
		int fights = 0;
		while ((unboundQuantity(owner, "WATER_PEARL") < 2 || unboundQuantity(owner, "TIDAL_CLAW_CAT_ESSENCE") < 2)
				&& fights < 400) {
			fight(owner, "VILLAGE");
			fights++;
		}
		assertTrue(unboundQuantity(owner, "WATER_PEARL") >= 2);
		assertTrue(unboundQuantity(owner, "TIDAL_CLAW_CAT_ESSENCE") >= 2);
		walk(owner, "CITY_PETS");
		int pearls = unboundQuantity(owner, "WATER_PEARL");
		int essence = unboundQuantity(owner, "TIDAL_CLAW_CAT_ESSENCE");
		var preview = evolutionPreview.preview(owner, Long.parseLong(cat.id()), Mode.UNBOUND_ONLY);
		var request = new CreatureEvolutionCommandService.Request(UUID.randomUUID().toString(), preview);
		var result = evolution.evolve(owner, Long.parseLong(cat.id()), request);
		assertEquals("STREAM_CLAW_CAT", result.species());
		assertEquals(result, evolution.evolve(owner, Long.parseLong(cat.id()), request));
		assertEquals(pearls - 2, unboundQuantity(owner, "WATER_PEARL"));
		assertEquals(essence - 2, unboundQuantity(owner, "TIDAL_CLAW_CAT_ESSENCE"));
		var evolved = creatureQueries.list(owner)
			.stream()
			.filter(pet -> pet.id().equals(cat.id()))
			.findFirst()
			.orElseThrow();
		assertEquals(cat.abilitySlot(), evolved.abilitySlot());
		assertEquals(cat.skills(), evolved.skills());
		System.out.printf("NATURAL_EVOLUTION family=CLAW_CAT materialFights=%d mode=UNBOUND_ONLY replayStable=true%n",
				fights);
	}

	private int unboundQuantity(long owner, String code) {
		return items.list(owner)
			.stream()
			.filter(item -> item.code().equals(code) && !item.bound())
			.mapToInt(InventoryItemView::quantity)
			.sum();
	}

	/** 升级后主动穿戴已经获得且职业适用的装备；不购买、赠送或凭空替换物品。 */
	private void equipEarnedGear(long owner) {
		var character = queries.find(owner);
		var selected = new LinkedHashMap<String, InventoryItemView>();
		items.list(owner)
			.stream()
			.filter(item -> item.slot() != null && item.quantity() > 0 && item.requiredLevel() <= character.level())
			.filter(item -> item.requiredProfession() == null
					|| item.requiredProfession().equals(character.profession()))
			.sorted(Comparator.comparingInt(InventoryItemView::requiredLevel))
			.forEach(item -> selected.put(item.slot(), item));
		for (var item : selected.values())
			if (!item.equipped())
				inventory.equip(owner, Long.parseLong(item.id()));
	}

	/** 自动操作者按公开任务步骤选择正确选项；仍由正式命令核查地点、前置、资产和战斗结果。 */
	private void satisfy(long owner, String fact) {
		if (fact.startsWith("VISIT:")) {
			String target = fact.substring(6);
			if (queries.find(owner).location().equals(target))
				walk(owner,
						world.place(target)
							.exits()
							.stream()
							.filter(next -> world.accessible(world.place(next), facts.codes(owner)::contains))
							.findFirst()
							.orElseThrow());
			walk(owner, target);
			return;
		}
		if (facts.codes(owner).contains(fact))
			return;
		if (fact.startsWith("SCENE:")) {
			var action = scenes.action(fact.substring(6));
			for (String prerequisite : action.requires())
				satisfy(owner, prerequisite);
			walk(owner, action.location());
			var result = sceneCommands.execute(owner, action.id(),
					new SceneActionCommandService.Request(UUID.randomUUID().toString(), queries.find(owner).revision(),
							action.answers().isEmpty() ? null : action.answers().getFirst()));
			assertTrue(result.completed(), action.id());
		}
		else if (fact.startsWith("VICTORY:")) {
			fight(owner, fact.substring(8));
		}
		else
			fail("尚未取得的任务前置：" + fact);
	}

	/** 任务遭遇与训练共用同一操作者，恢复和往返全部经过正式相邻道路。 */
	private void fight(long owner, String encounterId) {
		fight(owner, encounterId, true);
	}

	private void fight(long owner, String encounterId, boolean recover) {
		var encounter = battles.encounter(encounterId);
		// 使用相邻道路抵达最近恢复设施，恢复后再返回任务战斗地点。
		var recovery = world.definition()
			.places()
			.stream()
			.filter(p -> p.services().contains("RECOVERY"))
			.filter(p -> world.accessible(p, facts.codes(owner)::contains))
			.min(Comparator.comparingInt(p -> route(owner, p.id()).size()))
			.orElseThrow();
		if (recover) {
			walk(owner, recovery.id());
			battleCommands.recover(owner);
		}
		walk(owner, encounter.location());
		var battle = battleCommands.start(owner, encounter.id(), UUID.randomUUID().toString());
		for (int turn = 0; turn < 60 && battle.status().equals("ACTIVE"); turn++) {
			String enemy = battle.units()
				.stream()
				.filter(u -> u.enemy() && u.hp() > 0 && !u.captured())
				.findFirst()
				.orElseThrow()
				.id();
			var orders = new ArrayList<Order>();
			var active = battle.units()
				.stream()
				.filter(u -> !u.enemy() && u.hp() > 0 && (u.hero() || u.position() < 2))
				.toList();
			var injured = active.stream()
				.min(Comparator.comparingInt(u -> u.hp() * 100 / u.stats().health()))
				.orElseThrow();
			for (var unit : active) {
				String action = "ATTACK", target = enemy;
				if (injured.hp() * 100 < injured.stats().health() * 45) {
					for (String healing : List.of("微疗", "治疗术", "HEALTH_POTION"))
						if (unit.actions().stream().anyMatch(a -> a.id().equals(healing) && a.available())) {
							action = healing;
							target = injured.id();
							break;
						}
				}
				if (action.equals("ATTACK")) {
					for (String skill : List.of("水弹", "雷牙", "火球术", "碎岩击", "风刃"))
						if (unit.actions().stream().anyMatch(a -> a.id().equals(skill) && a.available())) {
							action = skill;
							break;
						}
				}
				orders.add(new Order(unit.id(), action, target));
			}
			battle = battleCommands.turn(owner, Long.parseLong(battle.id()), battle.round(), orders, false);
		}
		assertEquals("WON", battle.status(), encounter.id() + ": " + battle.log());
	}

	private void walk(long owner, String target) {
		for (String next : route(owner, target))
			characters.move(owner, next, queries.find(owner).revision());
	}

	/** 只沿已开放的相邻边寻找路线；不能把测试寻路变成服务端传送。 */
	private List<String> route(long owner, String target) {
		String current = queries.find(owner).location();
		var completed = facts.codes(owner);
		var queue = new ArrayDeque<List<String>>();
		var visited = new HashSet<String>();
		queue.add(List.of(current));
		visited.add(current);
		while (!queue.isEmpty()) {
			var path = queue.remove();
			String last = path.getLast();
			if (last.equals(target))
				return path.subList(1, path.size());
			for (String next : world.place(last).exits()) {
				if (!world.accessible(world.place(next), completed::contains) || !visited.add(next))
					continue;
				var extended = new ArrayList<>(path);
				extended.add(next);
				queue.add(extended);
			}
		}
		throw new AssertionError("没有已开放路线：" + current + " -> " + target);
	}

}
