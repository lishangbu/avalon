package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.AvalonApplication;
import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.battle.application.model.BattleView;
import io.github.lishangbu.avalon.battle.domain.BattleState.Order;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.command.SceneActionCommandService;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 正式开篇任务、现场调查、捕获登记及零资源恢复验收；前置事实通过真实应用命令产生。
 */
@SpringBootTest(classes = AvalonApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrightOpeningQuestRuntimeTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	QuestQueryService questsRead;

	@Autowired
	QuestCommandService quests;

	@Autowired
	QuestAcceptCommandService questAccepts;

	@Autowired
	BattleCommandService battles;

	@Autowired
	CharacterFactQueryService facts;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService creatureQueries;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService inventoryQueries;

	@Autowired
	SceneActionCommandService sceneActions;

	@Test
	void openingUsesFormalQueryCommandAndIdempotentReceipt() {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		String username = "bq_" + suffix;
		registrations.register(username, "Bright-test-123", "正式开篇测试");
		long owner = accounts.accountId(username);
		characters.create(owner, "候选" + suffix, "WARRIOR");
		characters.move(owner, "STONE_PATH", characterQueries.find(owner).revision());
		characters.move(owner, "SQUARE", characterQueries.find(owner).revision());

		var before = questsRead.state(owner);
		var first = before.available().stream().filter(action -> action.id().equals("BQ001")).findFirst().orElseThrow();
		assertFalse(first.interactable());
		assertTrue(first.acceptAvailable());
		assertTrue(before.completed().isEmpty());
		accept(owner, "BQ001");

		var result = quests.interact(owner, "BQ001");
		assertEquals(result, quests.interact(owner, "BQ001"));
		assertTrue(questsRead.state(owner).completed().contains("BQ001"));
		assertEquals(100, characterQueries.find(owner).experience());
	}

	@Test
	void capturedCreatureRegistrationProducesTheBq007Fact() {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		String username = "bq_capture_" + suffix;
		registrations.register(username, "Bright-test-123", "候选捕获测试");
		long owner = accounts.accountId(username);
		characters.create(owner, "候选捕获" + suffix, "WARRIOR");
		move(owner, "STONE_PATH");
		move(owner, "SQUARE");
		accept(owner, "BQ001");
		quests.interact(owner, "BQ001");
		move(owner, "ELDER_HOUSE");
		accept(owner, "BQ002");
		quests.interact(owner, "BQ002");
		move(owner, "SQUARE");
		move(owner, "WEAPON_SHOP");
		accept(owner, "BQ003");
		quests.interact(owner, "BQ003");
		move(owner, "SQUARE");
		move(owner, "PET_CENTER");
		accept(owner, "BQ004");
		quests.interact(owner, "BQ004");
		accept(owner, "BQ005");
		var pet = creatureQueries.list(owner).getFirst();
		// 生成时确定槽位；重放结契任务不能重抽特性，也不能生成第二只初始伙伴。
		assertTrue(Set.of("A", "B", "H").contains(pet.abilitySlot()));
		quests.interact(owner, "BQ004");
		assertEquals(List.of(pet), creatureQueries.list(owner));
		var gear = inventoryQueries.list(owner)
			.stream()
			.filter(item -> item.code().equals("STARTER_WEAPON"))
			.findFirst()
			.orElseThrow();
		creatures.position(owner, Long.parseLong(pet.id()), pet.partyPosition());
		creatures.viewedSkills(owner, Long.parseLong(pet.id()));
		for (var equipment : inventoryQueries.list(owner))
			if (equipment.slot() != null)
				inventory.equip(owner, Long.parseLong(equipment.id()));
		quests.interact(owner, "BQ005");
		move(owner, "SQUARE");
		move(owner, "STONE_PATH");
		move(owner, "RIVERSIDE");
		accept(owner, "BQ006");
		scene(owner, "BQ006_STONE_TRACE_A");
		scene(owner, "BQ006_STONE_TRACE_B");
		quests.interact(owner, "BQ006");
		move(owner, "STONE_PATH");
		move(owner, "SQUARE");
		move(owner, "PET_CENTER");
		questAccepts.accept(owner, "BQ007", new QuestAcceptCommandService.Request(UUID.randomUUID().toString(),
				characterQueries.find(owner).revision()));
		long captured = OpeningWildTraining.capture(owner, characters, characterQueries, battles, creatureQueries);
		creatures.position(owner, captured, 1);
		creatures.registerCapture(owner);
		assertTrue(facts.codes(owner).contains("CAPTURE:BQ007_REGISTERED"));
		quests.interact(owner, "BQ007");
		move(owner, "SQUARE");
		scene(owner, "BQ008_COLLECT_STONES");
		quests.interact(owner, "BQ008");
		move(owner, "PET_CENTER");
		accept(owner, "BQ009");
		battles.recover(owner);
		scene(owner, "BQ009_SUPPLY_REVIEW");
		quests.interact(owner, "BQ009");
		OpeningWildTraining.toLevel(owner, 5, characters, characterQueries, battles);
		move(owner, "SQUARE");
		move(owner, "STONE_PATH");
		move(owner, "RIVERSIDE");
		move(owner, "FOREST_EDGE");
		scene(owner, "BQ010_CART_TRACE_A");
		scene(owner, "BQ010_CART_TRACE_B");
		quests.interact(owner, "BQ010");
		move(owner, "CAVE_ENTRANCE");
		scene(owner, "BQ011_FOOTPRINTS");
		scene(owner, "BQ011_ATTACK_MARKS");
		battles.start(owner, "CAVE_BOSS", UUID.randomUUID().toString());
		assertTrue(questsRead.done(owner, "BQ011"));
		assertTrue(facts.codes(owner).contains("READINESS:BQ011_CONFIRMED"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "WARRIOR", "MAGE", "PRIEST", "RANGER" })
	void caveBossCanBeClearedByTheTelegraphDrivenManualFlow(String profession) {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		String username = "bq_boss_" + suffix;
		registrations.register(username, "Bright-test-123", "候选首领测试");
		long owner = accounts.accountId(username);
		characters.create(owner, "首领" + suffix, profession);
		move(owner, "STONE_PATH");
		move(owner, "SQUARE");
		accept(owner, "BQ001");
		quests.interact(owner, "BQ001");
		move(owner, "ELDER_HOUSE");
		accept(owner, "BQ002");
		quests.interact(owner, "BQ002");
		move(owner, "SQUARE");
		move(owner, "WEAPON_SHOP");
		accept(owner, "BQ003");
		quests.interact(owner, "BQ003");
		move(owner, "SQUARE");
		move(owner, "PET_CENTER");
		accept(owner, "BQ004");
		quests.interact(owner, "BQ004");
		accept(owner, "BQ005");
		var pet = creatureQueries.list(owner).getFirst();
		creatures.position(owner, Long.parseLong(pet.id()), pet.partyPosition());
		creatures.viewedSkills(owner, Long.parseLong(pet.id()));
		var gear = inventoryQueries.list(owner)
			.stream()
			.filter(item -> item.code().equals("STARTER_WEAPON"))
			.findFirst()
			.orElseThrow();
		for (var equipment : inventoryQueries.list(owner))
			if (equipment.slot() != null)
				inventory.equip(owner, Long.parseLong(equipment.id()));
		quests.interact(owner, "BQ005");
		move(owner, "SQUARE");
		move(owner, "STONE_PATH");
		move(owner, "RIVERSIDE");
		accept(owner, "BQ006");
		scene(owner, "BQ006_STONE_TRACE_A");
		scene(owner, "BQ006_STONE_TRACE_B");
		quests.interact(owner, "BQ006");
		move(owner, "STONE_PATH");
		move(owner, "SQUARE");
		move(owner, "PET_CENTER");
		questAccepts.accept(owner, "BQ007", new QuestAcceptCommandService.Request(UUID.randomUUID().toString(),
				characterQueries.find(owner).revision()));
		long captured = OpeningWildTraining.capture(owner, characters, characterQueries, battles, creatureQueries);
		creatures.position(owner, captured, 1);
		creatures.registerCapture(owner);
		assertTrue(facts.codes(owner).contains("CAPTURE:BQ007_REGISTERED"));
		quests.interact(owner, "BQ007");
		move(owner, "SQUARE");
		scene(owner, "BQ008_COLLECT_STONES");
		quests.interact(owner, "BQ008");
		move(owner, "PET_CENTER");
		accept(owner, "BQ009");
		battles.recover(owner);
		scene(owner, "BQ009_SUPPLY_REVIEW");
		quests.interact(owner, "BQ009");
		OpeningWildTraining.toLevel(owner, 5, characters, characterQueries, battles);
		move(owner, "SQUARE");
		move(owner, "STONE_PATH");
		move(owner, "RIVERSIDE");
		move(owner, "FOREST_EDGE");
		scene(owner, "BQ010_CART_TRACE_A");
		scene(owner, "BQ010_CART_TRACE_B");
		quests.interact(owner, "BQ010");
		move(owner, "CAVE_ENTRANCE");
		scene(owner, "BQ011_FOOTPRINTS");
		scene(owner, "BQ011_ATTACK_MARKS");
		var battle = battles.start(owner, "CAVE_BOSS", UUID.randomUUID().toString());
		for (int attempt = 0; attempt < 60 && battle.status().equals("ACTIVE"); attempt++) {
			String bossId = battle.units().stream().filter(BattleView.UnitView::enemy).findFirst().orElseThrow().id();
			boolean danger = battle.telegraph() != null && battle.telegraph().contains("即将");
			var orders = new ArrayList<Order>();
			for (var unit : battle.units()) {
				if (unit.enemy() || unit.hp() <= 0 || (!unit.hero() && unit.position() >= 2))
					continue;
				var injured = battle.units()
					.stream()
					.filter(member -> !member.enemy() && member.hp() > 0 && (member.hero() || member.position() < 2))
					.min(Comparator.comparingInt(member -> member.hp() * 100 / member.stats().health()))
					.orElse(unit);
				String action = danger ? "DEFEND" : "ATTACK";
				String target = danger ? unit.id() : bossId;
				if (unit.hero() && injured.hp() * 100 < injured.stats().health() * 45
						&& unit.actions()
							.stream()
							.anyMatch(value -> value.id().equals("HEALTH_POTION") && value.available())) {
					action = "HEALTH_POTION";
					target = injured.id();
				}
				else if (!unit.hero() && injured.hp() * 100 < injured.stats().health() * 55
						&& unit.actions().stream().anyMatch(value -> value.id().equals("微疗") && value.available())) {
					action = "微疗";
					target = injured.id();
				}
				if (!danger && action.equals("ATTACK")) {
					var skill = unit.actions()
						.stream()
						.filter(value -> value.available() && value.target().equals("ENEMY")
								&& !value.id().equals("ATTACK"))
						.findFirst();
					if (skill.isPresent())
						action = skill.get().id();
				}
				orders.add(new Order(unit.id(), action, target));
			}
			battle = battles.turn(owner, Long.parseLong(battle.id()), battle.round(), orders, false);
		}
		assertEquals("WON", battle.status());
		assertTrue(questsRead.done(owner, "BQ012"));
		assertTrue(inventoryQueries.list(owner).stream().anyMatch(item -> item.code().equals("CAVE_WEAPON")));
		quests.interact(owner, "BQ012");
		move(owner, "FOREST_EDGE");
		move(owner, "RIVERSIDE");
		move(owner, "STONE_PATH");
		move(owner, "SQUARE");
		quests.interact(owner, "BQ013");
		move(owner, "ELDER_HOUSE");
		quests.interact(owner, "BQ014");
		move(owner, "SQUARE");
		move(owner, "STONE_PATH");
		move(owner, "RIVERSIDE");
		move(owner, "FOREST_EDGE");
		move(owner, "PLAIN_SIGN");
		accept(owner, "BQ015");
		scene(owner, "BQ015_SIGN");
		scene(owner, "BQ015_WATER_ROUTE");
		quests.interact(owner, "BQ015");
		move(owner, "STATION_OUTSKIRTS");
		scene(owner, "BQ016_INSPECT");
		scene(owner, "BQ016_WATER");
		scene(owner, "BQ016_DELIVER_CARE");
		quests.interact(owner, "BQ016");
		move(owner, "ACADEMY");
		scene(owner, "BQ017_ARRIVAL");
		scene(owner, "BQ017_PRESENT_INTRO");
		quests.interact(owner, "BQ017");
		assertTrue(questsRead.done(owner, "BQ017"));
	}

	private void move(long owner, String destination) {
		characters.move(owner, destination, characterQueries.find(owner).revision());
	}

	private void accept(long owner, String step) {
		questAccepts.accept(owner, step, new QuestAcceptCommandService.Request(UUID.randomUUID().toString(),
				characterQueries.find(owner).revision()));
	}

	private void scene(long owner, String action) {
		sceneActions.execute(owner, action, new SceneActionCommandService.Request(UUID.randomUUID().toString(),
				characterQueries.find(owner).revision(), null));
	}

}
