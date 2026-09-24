package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.command.SceneActionCommandService;
import jakarta.persistence.EntityManager;
import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.battle.application.model.BattleView;
import io.github.lishangbu.avalon.battle.domain.BattleState.Order;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.persistence.repository.BattleRecordRepository;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 园圃从开篇完成后独立开放；观察、判断与奖励必须由玩家实际操作推进。 */
class GardenStoryTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	CharacterQueryService queries;

	@Autowired
	QuestCommandService quests;

	@Autowired
	QuestAcceptCommandService acceptance;

	@Autowired
	SceneActionCommandService scenes;

	@Autowired
	InventoryQueryService inventory;

	@Autowired
	EntityManager em;

	@Autowired
	BattleCommandService battles;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	GrowthRules growth;

	@Autowired
	InventoryCommandService items;

	@Autowired
	BattleRecordRepository records;

	@Autowired
	ObjectMapper json;

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void gardenRequiresObservationsAndAllowsCorrectingConclusionsWithoutConsumingInventory(boolean capture) {
		String name = "garden_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Garden-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "MAGE");
		transactions.run(() -> {
			facts.record(owner, "QUEST:BQ024");
			facts.record(owner, "QUEST:BQ025");
			facts.record(owner, "QUEST:BQ029");
			em.createNativeQuery("UPDATE player_character SET location='GNOME_CITY' WHERE id=:id")
				.setParameter("id", characters.lock(owner).id())
				.executeUpdate();
		});
		assertThrows(BusinessException.class, () -> move(owner, "GNOME_GARDEN"));
		transactions.run(() -> {
			facts.record(owner, "QUEST:BQ030");
			em.createNativeQuery(
					"INSERT INTO quest_receipt (id, character_id, step, quest_code, completed, response, created_at)"
							+ " VALUES (:id, :id, 'BQ030', 'BS04', true, '{}', now())")
				.setParameter("id", characters.lock(owner).id())
				.executeUpdate();
		});
		// 不伪造罗温任务回执：园圃必须能够作为开篇后的另一条独立路线。
		assertThrows(BusinessException.class, () -> action(owner, "BQ040_RECORD", null));
		move(owner, "GNOME_GARDEN");
		acceptance.accept(owner, "BQ040",
				new QuestAcceptCommandService.Request(UUID.randomUUID().toString(), queries.find(owner).revision()));
		var before = inventory.list(owner);
		assertThrows(BusinessException.class, () -> action(owner, "BQ040_BED", null));
		action(owner, "BQ040_RECORD", null);
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ040"));
		action(owner, "BQ040_BED", null);
		claim(owner, "BQ040");
		for (String plot : List.of("SHADE", "WATER"))
			action(owner, "BQ041_" + plot, null);
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ041"));
		action(owner, "BQ041_HIGH", null);
		claim(owner, "BQ041");
		assertThrows(BusinessException.class, () -> action(owner, "BQ042_REPORT", "FORAGE"));
		action(owner, "BQ042_ROUTE", null);
		action(owner, "BQ042_STOP", null);
		for (String answer : List.of("PEST", "DISASTER")) {
			assertFalse(scenes.execute(owner, "BQ042_REPORT", request(owner, answer)).completed());
			assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ042"));
		}
		action(owner, "BQ042_REPORT", "FORAGE");
		claim(owner, "BQ042");
		assertEquals(15593, queries.find(owner).experience());
		assertThrows(BusinessException.class, () -> action(owner, "BQ043_UPPER", null));
		move(owner, "GNOME_CITY");
		move(owner, "CITY_OUTSKIRTS");
		assertThrows(BusinessException.class, () -> action(owner, "BQ043_BLOCKAGE", null));
		action(owner, "BQ043_UPPER", null);
		assertThrows(BusinessException.class, () -> action(owner, "BQ043_BLOCKAGE", null));
		action(owner, "BQ043_BRANCH", null);
		action(owner, "BQ043_BLOCKAGE", null);
		for (String answer : List.of("MAGIC", "SOLVED")) {
			assertFalse(scenes.execute(owner, "BQ043_REPORT", request(owner, answer)).completed());
			assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ043"));
		}
		action(owner, "BQ043_REPORT", "LOCAL");
		claim(owner, "BQ043");
		assertEquals(21830, queries.find(owner).experience());
		assertEquals(before, inventory.list(owner));
		assertThrows(BusinessException.class,
				() -> battles.start(owner, "GARDEN_WORK_ROUTE", UUID.randomUUID().toString()));
		action(owner, "BQ044_NEST", null);
		assertThrows(BusinessException.class, () -> action(owner, "BQ044_ROUTE", "OUTER"));
		action(owner, "BQ044_OUTER", null);
		assertFalse(scenes.execute(owner, "BQ044_ROUTE", request(owner, "NEST")).completed());
		action(owner, "BQ044_ROUTE", "OUTER");
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ044"));
		// 此处验证任务战斗结算，使用12级队伍夹具，不把夹具升级算作自然成长证据。
		transactions.run(() -> characters.grantExperience(owner,
				Math.toIntExact(growth.threshold(12) - queries.find(owner).experience())));
		for (int i = 0; i < 2; i++) {
			int index = i;
			long pet = transactions.call(() -> creatures.capture(owner, "GARDEN_TEST:" + index, "CLAW_CAT", 12, "A",
					"完好防护", List.of("水弹", "微疗")));
			creatures.position(owner, pet, i);
		}
		move(owner, "GNOME_CITY");
		move(owner, "CITY_PETS");
		battles.recover(owner);
		move(owner, "GNOME_CITY");
		move(owner, "CITY_OUTSKIRTS");
		var battle = battles.start(owner, "GARDEN_WORK_ROUTE", UUID.randomUUID().toString());
		if (capture) {
			// 先通过正式撤退结算验证失败不会生成安全事实，记录保留后可重试。
			battle = battles.turn(owner, Long.parseLong(battle.id()), battle.round(), List.of(), true);
			assertEquals("SURRENDERED", battle.status());
			assertEquals("GNOME_CITY", queries.find(owner).location());
			move(owner, "CITY_PETS");
			battles.recover(owner);
			move(owner, "GNOME_CITY");
			move(owner, "CITY_OUTSKIRTS");
			assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ044"));
			transactions.run(() -> {
				facts.record(owner, "QUEST:BQ007");
				em.createNativeQuery(
						"INSERT INTO quest_receipt (id, character_id, step, quest_code, completed, response, created_at)"
								+ " VALUES (-:id, :id, 'BQ007', 'BS01', true, '{}', now())")
					.setParameter("id", characters.lock(owner).id())
					.executeUpdate();
				items.grant(owner, "GARDEN_CAPTURE_TEST", "SEAL_II", 1, false);
			});
			battle = battles.start(owner, "GARDEN_WORK_ROUTE", UUID.randomUUID().toString());
			var target = battle.units().stream().filter(BattleView.UnitView::enemy).findFirst().orElseThrow();
			double chance = target.captures()
				.stream()
				.filter(ball -> ball.code().equals("SEAL_II"))
				.findFirst()
				.orElseThrow()
				.chance();
			// 固定成功抽样以覆盖捕获落库；不改目标生命、捕捉公式或敌人战斗数值。
			long seed = 0;
			for (; seed < 1000000; seed++) {
				var random = new Random(seed + battle.round());
				Collections.shuffle(new ArrayList<>(List.of(1, 2, 3, 4)), random);
				if (random.nextDouble() < chance && random.nextDouble() < chance && random.nextDouble() < chance)
					break;
			}
			assertTrue(seed < 1000000);
			long chosenSeed = seed;
			long battleId = Long.parseLong(battle.id());
			transactions.run(() -> {
				var stored = records.get(battleId);
				var state = json.readValue(stored.getState(), BattleState.class);
				state.setSeed(chosenSeed);
				stored.setState(json.writeValueAsString(state));
			});
			long xp = queries.find(owner).experience();
			long gold = queries.find(owner).gold();
			var orders = battle.units()
				.stream()
				.filter(unit -> !unit.enemy())
				.map(unit -> new Order(unit.id(), unit.hero() ? "SEAL_II" : "DEFEND",
						unit.hero() ? target.id() : unit.id()))
				.toList();
			battle = battles.turn(owner, battleId, battle.round(), orders, false);
			assertEquals("WON", battle.status());
			assertTrue(battle.units().stream().anyMatch(unit -> unit.enemy() && unit.captured()));
			assertEquals(xp, queries.find(owner).experience(), "捕获没有击杀经验");
			assertEquals(gold + 10, queries.find(owner).gold(), "首次收集雷蜥只发10金币图鉴奖励，没有击杀金币");
			assertEquals(2,
					inventory.list(owner)
						.stream()
						.filter(item -> item.code().equals("SEAL_I"))
						.mapToInt(item -> item.quantity())
						.sum(),
					"利爪猫与雷蜥各发一次首收封印球");
			assertTrue(inventory.list(owner)
				.stream()
				.noneMatch(item -> item.code().equals("SEAL_II") && item.quantity() > 0));
		}
		for (int round = 0; round < 30 && battle.status().equals("ACTIVE"); round++) {
			String enemy = battle.units().stream().filter(BattleView.UnitView::enemy).findFirst().orElseThrow().id();
			var orders = battle.units()
				.stream()
				.filter(unit -> !unit.enemy() && unit.hp() > 0 && (unit.hero() || unit.position() < 2))
				.map(unit -> new Order(unit.id(), "ATTACK", enemy))
				.toList();
			battle = battles.turn(owner, Long.parseLong(battle.id()), battle.round(), orders, false);
		}
		assertEquals("WON", battle.status());
		long beforeReward = queries.find(owner).experience();
		claim(owner, "BQ044");
		assertEquals(beforeReward + 9355, queries.find(owner).experience());
		assertThrows(BusinessException.class,
				() -> battles.start(owner, "GARDEN_WORK_ROUTE", UUID.randomUUID().toString()));
		var supplies = inventory.list(owner);
		move(owner, "GNOME_CITY");
		move(owner, "GNOME_GARDEN");
		action(owner, "BQ045_SHELTER", null);
		assertThrows(BusinessException.class, () -> action(owner, "BQ045_RECORD", null));
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ045"));
		action(owner, "BQ045_CONTROL", null);
		action(owner, "BQ045_RECORD", null);
		claim(owner, "BQ045");
		assertThrows(BusinessException.class, () -> action(owner, "BQ046_WATER", null));
		assertThrows(BusinessException.class, () -> action(owner, "BQ046_CLEAR", null));
		move(owner, "GNOME_CITY");
		move(owner, "CITY_OUTSKIRTS");
		action(owner, "BQ046_CLEAR", null);
		move(owner, "GNOME_CITY");
		move(owner, "GNOME_GARDEN");
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ046"));
		action(owner, "BQ046_WATER", null);
		claim(owner, "BQ046");
		action(owner, "BQ047_LEAVES", null);
		assertThrows(BusinessException.class, () -> action(owner, "BQ047_REPORT", "LIMITED"));
		action(owner, "BQ047_VISITORS", null);
		for (String answer : List.of("ALL", "VISITORS"))
			assertFalse(scenes.execute(owner, "BQ047_REPORT", request(owner, answer)).completed());
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ047"));
		action(owner, "BQ047_REPORT", "LIMITED");
		claim(owner, "BQ047");
		action(owner, "BQ048_DATE", null);
		assertThrows(BusinessException.class, () -> action(owner, "BQ048_NOTES", null));
		action(owner, "BQ048_PLACE", null);
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ048"));
		action(owner, "BQ048_NOTES", null);
		claim(owner, "BQ048");
		assertEquals(beforeReward + 9355 + 21830, queries.find(owner).experience());
		assertEquals(supplies, inventory.list(owner), "园圃共用工具与记录不消耗或占用玩家背包");
		move(owner, "GNOME_CITY");
		move(owner, "CITY_OUTSKIRTS");
		assertThrows(BusinessException.class, () -> move(owner, "GNOME_OLD_TRAIL"));
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ049"));
		acceptance.accept(owner, "BQ049",
				new QuestAcceptCommandService.Request(UUID.randomUUID().toString(), queries.find(owner).revision()));
		action(owner, "BQ049_SIGN", null);
		claim(owner, "BQ049");
		assertThrows(BusinessException.class, () -> action(owner, "BQ050_OVERHANG", null));
		move(owner, "GNOME_OLD_TRAIL");
		action(owner, "BQ050_OVERHANG", null);
		assertThrows(BusinessException.class, () -> action(owner, "BQ050_BYPASS", null));
		action(owner, "BQ050_SLOPE", null);
		action(owner, "BQ050_BYPASS", null);
		claim(owner, "BQ050");
		assertThrows(BusinessException.class, () -> action(owner, "BQ051_PASSAGE", null));
		action(owner, "BQ051_TRACKS", null);
		action(owner, "BQ051_PASSAGE", null);
		claim(owner, "BQ051");
		for (String answer : List.of("PASSAGE", "SLOPE"))
			assertFalse(scenes.execute(owner, "BQ052_SITE", request(owner, answer)).completed());
		assertThrows(BusinessException.class, () -> action(owner, "BQ052_BOUNDARY", null));
		action(owner, "BQ052_SITE", "FLAT");
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ052"));
		action(owner, "BQ052_BOUNDARY", null);
		claim(owner, "BQ052");
		assertEquals(beforeReward + 9355 + 21830 * 2, queries.find(owner).experience());
		assertEquals(supplies, inventory.list(owner));
		assertThrows(BusinessException.class, () -> move(owner, "GNOME_MINE_GATE"));
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ053"));
		move(owner, "CITY_OUTSKIRTS");
		move(owner, "GNOME_CITY");
		move(owner, "CITY_PETS");
		battles.recover(owner);
		move(owner, "GNOME_CITY");
		move(owner, "CITY_OUTSKIRTS");
		move(owner, "GNOME_OLD_TRAIL");
		battle = battles.start(owner, "OLD_TRAIL_GUARD", UUID.randomUUID().toString());
		for (int round = 0; round < 40 && battle.status().equals("ACTIVE"); round++) {
			String enemy = battle.units().stream().filter(BattleView.UnitView::enemy).findFirst().orElseThrow().id();
			var orders = battle.units()
				.stream()
				.filter(unit -> !unit.enemy() && unit.hp() > 0 && (unit.hero() || unit.position() < 2))
				.map(unit -> new Order(unit.id(),
						unit.actions().stream().anyMatch(a -> a.id().equals("水弹") && a.available()) ? "水弹" : "ATTACK",
						enemy))
				.toList();
			battle = battles.turn(owner, Long.parseLong(battle.id()), battle.round(), orders, false);
		}
		assertEquals("WON", battle.status());
		long trialXp = queries.find(owner).experience();
		claim(owner, "BQ053");
		assertThrows(BusinessException.class,
				() -> battles.start(owner, "OLD_TRAIL_GUARD", UUID.randomUUID().toString()));
		assertThrows(BusinessException.class, () -> action(owner, "BQ054_MIDDLE", null));
		action(owner, "BQ054_FIRST", null);
		assertThrows(BusinessException.class, () -> action(owner, "BQ054_LAST", null));
		action(owner, "BQ054_MIDDLE", null);
		action(owner, "BQ054_LAST", null);
		assertThrows(BusinessException.class, () -> move(owner, "GNOME_MINE_GATE"));
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ054"));
		action(owner, "BQ054_MARKERS", null);
		claim(owner, "BQ054");
		assertThrows(BusinessException.class, () -> action(owner, "BQ055_RETURN", null));
		move(owner, "GNOME_MINE_GATE");
		action(owner, "BQ055_RETURN", null);
		claim(owner, "BQ055");
		assertThrows(BusinessException.class, () -> action(owner, "BQ056_CREATURES", null));
		move(owner, "GNOME_OLD_TRAIL");
		action(owner, "BQ056_CREATURES", null);
		assertThrows(BusinessException.class, () -> action(owner, "BQ056_HANDOVER", null));
		action(owner, "BQ056_WALKWAY", null);
		action(owner, "BQ056_HANDOVER", null);
		claim(owner, "BQ056");
		assertEquals(trialXp + 24948, queries.find(owner).experience());
		move(owner, "GNOME_MINE_GATE");
		move(owner, "GNOME_OLD_TRAIL");
	}

	private void claim(long owner, String step) {
		var receipt = quests.interact(owner, step);
		assertEquals(receipt, quests.interact(owner, step));
	}

	private void move(long owner, String destination) {
		characters.move(owner, destination, queries.find(owner).revision());
	}

	private SceneActionCommandService.Request request(long owner, String answer) {
		return new SceneActionCommandService.Request(UUID.randomUUID().toString(), queries.find(owner).revision(),
				answer);
	}

	private void action(long owner, String id, String answer) {
		assertTrue(scenes.execute(owner, id, request(owner, answer)).completed());
	}

}
