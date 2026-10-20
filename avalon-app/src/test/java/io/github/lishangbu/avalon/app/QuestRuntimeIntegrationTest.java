package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestDialogueCatalog;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.quest.model.QuestEffect;

import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.quest.model.QuestPolicy;
import io.github.lishangbu.avalon.content.quest.model.QuestRepeatPolicy;
import io.github.lishangbu.avalon.content.quest.model.QuestRule;
import io.github.lishangbu.avalon.content.quest.model.QuestRulesDefinition;

import io.github.lishangbu.avalon.content.quest.model.QuestType;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.quest.QuestInstance;
import io.github.lishangbu.avalon.quest.model.QuestBranch;
import io.github.lishangbu.avalon.quest.model.QuestDelivery;
import io.github.lishangbu.avalon.quest.model.QuestEvent;
import io.github.lishangbu.avalon.quest.model.QuestMeasure;
import io.github.lishangbu.avalon.quest.model.QuestObjective;
import io.github.lishangbu.avalon.quest.model.QuestStatus;
import io.github.lishangbu.avalon.quest.model.QuestTimeout;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import io.github.lishangbu.avalon.world.quest.QuestClock;
import io.github.lishangbu.avalon.world.quest.QuestRuntimeService;
import io.github.lishangbu.avalon.world.quest.model.QuestInstanceRequest;
import io.github.lishangbu.avalon.world.quest.model.QuestOperation;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import io.github.lishangbu.avalon.quest.model.*;
import io.github.lishangbu.avalon.content.quest.model.QuestReception;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 正式Spring宿主和PostgreSQL验证新实例规则、奖励与状态共同回滚，以及实例命令的归属和防重。
 * 临时任务拥有完整目录和规则，通过生产接取入口创建实例，不直接伪造奖励或进度。
 */
class QuestRuntimeIntegrationTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registration;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService queries;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	QuestAcceptCommandService acceptance;

	@Autowired
	QuestRuntimeService runtime;

	@Autowired
	QuestQueryService questQueries;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService inventoryCommands;

	@Autowired
	io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService inventoryQueries;

	@Autowired
	io.github.lishangbu.avalon.world.application.command.SceneActionCommandService sceneCommands;

	@Autowired
	io.github.lishangbu.avalon.battle.application.command.BattleCommandService openingBattles;

	@Autowired
	io.github.lishangbu.avalon.battle.application.query.BattleQueryService openingBattleQueries;

	@Autowired
	io.github.lishangbu.avalon.battle.application.command.TrainingCommandService openingTraining;

	@Autowired
	io.github.lishangbu.avalon.battle.persistence.repository.BattleRecordRepository openingBattleRecords;

	@Autowired
	io.github.lishangbu.avalon.battle.domain.BattleEngine openingEngine;

	@Autowired
	tools.jackson.databind.ObjectMapper openingCodec;

	@Autowired
	io.github.lishangbu.avalon.battle.application.query.EncounterAccess encounterAccess;

	@Autowired
	io.github.lishangbu.avalon.content.battle.BattleCatalog battleCatalog;

	@Autowired
	io.github.lishangbu.avalon.world.persistence.repository.QuestRepository completionReceipts;

	/** 为独立任务测试建立正式前置完成回执，不把普通剧情事实冒充任务完成。 */
	private void completeGate(long owner, String step) {
		if (questQueries.done(owner, step))
			return;
		transactions.run(() -> {
			var receipt = new io.github.lishangbu.avalon.world.persistence.entity.QuestReceipt();
			receipt.setCharacterId(queries.requireId(owner));
			receipt.setStep(step);
			receipt.setQuestCode(catalog.step(step).quest());
			receipt.setCompleted(true);
			receipt.setResponse("{}");
			receipt.setCreatedAt(Instant.now());
			completionReceipts.save(receipt);
			facts.record(owner, "QUEST:" + step);
		});
	}

	/** 章节隔离用例的角色已经完成新开篇；自然开篇用例仍从注册开始。 */
	private void completeOpening(long owner) {
		for (String gate : List.of("O013", "O014", "O015"))
			completeGate(owner, gate);
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "U025", "S025" })
	void fireOpeningRequiresBothCitiesAndJoinsParallelObjectives(String first) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeGate(owner, "N025");
		completeGate(owner, first);
		String origin = first.equals("U025") ? "UNDINE_CITY" : "SYLPH_CITY";
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", origin, queries.requireId(owner));
		assertThrows(RuntimeException.class, () -> openingMove(owner, "SALAMANDER_CITY"));
		completeGate(owner, first.equals("U025") ? "S025" : "U025");
		openingMove(owner, "SALAMANDER_CITY");
		var arrival = acceptOpening(owner, "F001");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(arrival, QuestOperation.DELIVER, "F001")));
		openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_CITY");
		runtime.command(owner, request(arrival, QuestOperation.DELIVER, "F001"));
		var clearing = acceptOpening(owner, "F002");
		var parts = acceptOpening(owner, "F003");
		assertThrows(RuntimeException.class, () -> acceptOpening(owner, "F004"));
		transactions.run(() -> {
			inventoryCommands.grant(owner, "FIRE_PARTS", "F_ANCIENT_PART", 5, false);
			for (int i = 0; i < 9; i++)
				facts.record(owner, "KILL_LOCATION:SALAMANDER_ASH_ROAD");
			facts.record(owner, "KILL_LOCATION:UNDINE_SHALLOWS");
		});
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(clearing, QuestOperation.DELIVER, "F002")));
		transactions.run(() -> facts.record(owner, "KILL_LOCATION:SALAMANDER_ASH_ROAD"));
		runtime.command(owner, request(clearing, QuestOperation.DELIVER, "F002"));
		assertThrows(RuntimeException.class, () -> acceptOpening(owner, "F004"));
		var handIn = request(parts, QuestOperation.DELIVER, "F003");
		runtime.command(owner, handIn);
		runtime.command(owner, handIn);
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("F_ANCIENT_PART"))
					.mapToInt(item -> item.quantity())
					.sum());
		var keeper = acceptOpening(owner, "F004");
		transactions.run(() -> facts.record(owner, "VICTORY:F_ROAD_KEEPER"));
		var victory = request(keeper, QuestOperation.DELIVER, "F004");
		runtime.command(owner, victory);
		runtime.command(owner, victory);
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("F_WORKSHOP_BODY"))
					.mapToInt(item -> item.quantity())
					.sum());
		var opening = acceptOpening(owner, "F005");
		openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_SEALED_WORKS");
		sceneCommands.execute(owner, "F005_OPEN",
				new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(UUID.randomUUID().toString(),
						queries.find(owner).revision(), null));
		openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_CITY");
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(opening, QuestOperation.DELIVER, "F005")).instance().status());
		for (int n = 6; n <= 10; n++) {
			String id = "F" + String.format("%03d", n);
			var investigation = acceptOpening(owner, id);
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(investigation, QuestOperation.DELIVER, id)));
			if (n == 8) {
				transactions.run(() -> facts.record(owner, "VICTORY:F_FACILITY_GUARD"));
			}
			else if (n == 9) {
				transactions.run(() -> inventoryCommands.grant(owner, "FIRE_ORE", "LIFE_ORE_0", 11, false));
				assertThrows(RuntimeException.class,
						() -> runtime.command(owner, request(investigation, QuestOperation.DELIVER, id)));
				assertEquals(11,
						inventoryQueries.list(owner)
							.stream()
							.filter(item -> item.code().equals("LIFE_ORE_0"))
							.mapToInt(item -> item.quantity())
							.sum());
				transactions.run(() -> inventoryCommands.grant(owner, "FIRE_WOOD", "LIFE_WOOD_0", 7, false));
			}
			else {
				openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_SEALED_WORKS");
				String action = id + (n == 6 ? "_RECORDS" : n == 7 ? "_BLUEPRINT" : "_CONNECTION");
				var operation = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
						UUID.randomUUID().toString(), queries.find(owner).revision(), null);
				var result = sceneCommands.execute(owner, action, operation);
				assertEquals(result, sceneCommands.execute(owner, action, operation));
				openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_CITY");
			}
			var delivery = request(investigation, QuestOperation.DELIVER, id);
			assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
			long experience = queries.find(owner).experience();
			long gold = queries.find(owner).gold();
			runtime.command(owner, delivery);
			assertEquals(experience, queries.find(owner).experience());
			assertEquals(gold, queries.find(owner).gold());
		}
		for (String material : List.of("LIFE_ORE_0", "LIFE_WOOD_0"))
			assertEquals(1,
					inventoryQueries.list(owner)
						.stream()
						.filter(item -> item.code().equals(material))
						.mapToInt(item -> item.quantity())
						.sum());
		assertTrue(inventoryQueries.list(owner)
			.stream()
			.noneMatch(item -> List.of("F_CORE_BLUEPRINT", "F_SEAL_CONNECTION").contains(item.code())));
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "FIGHT", "REPAIR" })
	void fireShutdownAllowsEitherPreparationAndRequiresBattleAndOperation(String option) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("U025", "S025", "F001", "F004", "F010"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", "SALAMANDER_CITY", queries.requireId(owner));
		var survey = acceptOpening(owner, "F011");
		openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_SEALED_WORKS");
		assertThrows(RuntimeException.class, () -> openingMove(owner, "F_CORE_APPROACH"));
		openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_CITY");
		runtime.command(owner, request(survey, QuestOperation.DELIVER, "F011"));
		transactions.run(() -> {
			inventoryCommands.grant(owner, "F_SHUTDOWN_ORE", "LIFE_ORE_0", 12, false);
			facts.record(owner, "VICTORY:F_BYPASS_GUARD");
		});
		var preparation = acceptOpening(owner, "F012_FIGHT");
		for (String choice : List.of("REPAIR", "FIGHT", option))
			runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(), preparation.id(),
					preparation.round(), QuestOperation.CHOOSE, null, "F_SHUTDOWN_ROUTE", choice));
		if (option.equals("FIGHT")) {
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(preparation, QuestOperation.DELIVER, "F012_FIGHT")));
			transactions.run(() -> facts.record(owner, "VICTORY:F_BYPASS_GUARD"));
		}
		var delivery = request(preparation, QuestOperation.DELIVER, "F012_" + option);
		runtime.command(owner, delivery);
		long xp = queries.find(owner).experience();
		runtime.command(owner, delivery);
		assertEquals(xp, queries.find(owner).experience());
		assertTrue(questQueries.done(owner, "F012"));
		assertEquals(option.equals("REPAIR") ? 2 : 12,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_ORE_0"))
					.mapToInt(item -> item.quantity())
					.sum());
		var clearing = acceptOpening(owner, "F013");
		transactions.run(() -> {
			for (int i = 0; i < 9; i++)
				facts.record(owner, "KILL_LOCATION:F_CORE_APPROACH");
			facts.record(owner, "KILL_LOCATION:SALAMANDER_ASH_ROAD");
		});
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(clearing, QuestOperation.DELIVER, "F013")));
		transactions.run(() -> facts.record(owner, "KILL_LOCATION:F_CORE_APPROACH"));
		runtime.command(owner, request(clearing, QuestOperation.DELIVER, "F013"));
		var shutdown = acceptOpening(owner, "F014");
		transactions.run(() -> facts.record(owner, "VICTORY:F_OVERFLOW_SPAWN"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(shutdown, QuestOperation.DELIVER, "F014")));
		openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_SEALED_WORKS", "F_CORE_APPROACH");
		var operation = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		var result = sceneCommands.execute(owner, "F014_SHUTDOWN", operation);
		assertEquals(result, sceneCommands.execute(owner, "F014_SHUTDOWN", operation));
		openingMove(owner, "SALAMANDER_SEALED_WORKS", "SALAMANDER_ASH_ROAD", "SALAMANDER_CITY");
		runtime.command(owner, request(shutdown, QuestOperation.DELIVER, "F014"));
		var unlock = acceptOpening(owner, "F015");
		openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_SEALED_WORKS", "F_CORE_APPROACH");
		sceneCommands.execute(owner, "F015_UNLOCK",
				new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(UUID.randomUUID().toString(),
						queries.find(owner).revision(), null));
		openingMove(owner, "SALAMANDER_SEALED_WORKS", "SALAMANDER_ASH_ROAD", "SALAMANDER_CITY");
		var finalDelivery = request(unlock, QuestOperation.DELIVER, "F015");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, finalDelivery).instance().status());
		xp = queries.find(owner).experience();
		runtime.command(owner, finalDelivery);
		assertEquals(xp, queries.find(owner).experience());
	}

	@Test
	void jointClosureArchivesRecordsAndRequiresAllFourNewCityVisits() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O014", "O013", "O014", "N025", "U025", "S025", "J005",
				"J014", "J020"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", "J_ABYSS_ENTRANCE", queries.requireId(owner));
		var core = acceptOpening(owner, "J021");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(core, QuestOperation.DELIVER, "J021")));
		var action = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		var result = sceneCommands.execute(owner, "J021_RECORD", action);
		assertEquals(result, sceneCommands.execute(owner, "J021_RECORD", action));
		runtime.command(owner, request(core, QuestOperation.DELIVER, "J021"));
		var leave = acceptOpening(owner, "J022");
		openingMove(owner, "J_CORE_PERIMETER", "SALAMANDER_CITY");
		runtime.command(owner, request(leave, QuestOperation.DELIVER, "J022"));
		var seal = acceptOpening(owner, "J023");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(seal, QuestOperation.DELIVER, "J023")));
		openingMove(owner, "J_CORE_PERIMETER");
		action = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(UUID.randomUUID().toString(),
				queries.find(owner).revision(), null);
		result = sceneCommands.execute(owner, "J023_SEAL", action);
		assertEquals(result, sceneCommands.execute(owner, "J023_SEAL", action));
		openingMove(owner, "SALAMANDER_CITY");
		var delivery = request(seal, QuestOperation.DELIVER, "J023");
		runtime.command(owner, delivery);
		runtime.command(owner, delivery);
		assertFalse(inventoryQueries.list(owner).stream().anyMatch(item -> item.code().equals("J_CORE_REPORT")));
		transactions.run(() -> {
			for (String city : List.of("GNOME_CITY", "UNDINE_CITY", "SYLPH_CITY", "SALAMANDER_CITY"))
				facts.record(owner, "VISIT:" + city);
		});
		var cities = acceptOpening(owner, "J024");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(cities, QuestOperation.DELIVER, "J024")));
		openingMove(owner, "SYLPH_CITY", "SALAMANDER_CITY", "UNDINE_CITY");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(cities, QuestOperation.DELIVER, "J024")));
		openingMove(owner, "GNOME_PORT_ROAD", "CITY_OUTSKIRTS", "GNOME_CITY", "CITY_OUTSKIRTS", "GNOME_PORT_ROAD",
				"UNDINE_CITY", "SALAMANDER_CITY");
		delivery = request(cities, QuestOperation.DELIVER, "J024");
		runtime.command(owner, delivery);
		runtime.command(owner, delivery);
		assertFalse(inventoryQueries.list(owner).stream().anyMatch(item -> item.code().equals("J_CLOSURE_REPORT")));
		var reward = acceptOpening(owner, "J025");
		delivery = request(reward, QuestOperation.DELIVER, "J025");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		long xp = queries.find(owner).experience(), gold = queries.find(owner).gold();
		runtime.command(owner, delivery);
		assertEquals(xp, queries.find(owner).experience());
		assertEquals(gold, queries.find(owner).gold());
		assertFalse(inventoryQueries.list(owner).stream().anyMatch(item -> item.code().equals("J_REWARD_VOUCHER")));
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("J_RESTORER_BODY"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@Test
	void selectedPetDeliveryRejectsMissingProtectedChangedAndForeignIndividualsAndReplaysOnce() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		transactions.run(() -> characters.grantExperience(owner, 7380));
		for (String gate : List.of("O014", "O015"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", "CITY_PETS", queries.requireId(owner));
		transactions.run(() -> {
			creatures.grant(owner, "KEEPER", "CLAW_CAT");
			creatures.grant(owner, "HAND_IN", "STONE_GOLEM");
			creatures.grant(owner, "KEEP_GOLEM", "STONE_GOLEM");
		});
		var golems = creatureQueries.list(owner).stream().filter(pet -> pet.species().equals("STONE_GOLEM")).toList();
		String selectedId = golems.getFirst().id();
		jdbc.update("UPDATE owned_creature SET party_position=100 WHERE id=?", Long.parseLong(selectedId));
		var state = acceptOpening(owner, "N043");
		long gold = queries.find(owner).gold();
		long other = create();
		transactions.run(() -> creatures.grant(other, "FOREIGN", "STONE_GOLEM"));
		var foreign = creatureQueries.list(other).getFirst();
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(),
						state.round(), QuestOperation.DELIVER, "N043", null, null, List.of(foreign))));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N043")));
		var selected = creatureQueries.list(owner)
			.stream()
			.filter(pet -> pet.id().equals(selectedId))
			.findFirst()
			.orElseThrow();
		var delivery = new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), state.round(),
				QuestOperation.DELIVER, "N043", null, null, List.of(selected));
		jdbc.update("UPDATE owned_creature SET party_position=3 WHERE id=?", Long.parseLong(selectedId));
		var reserve = creatureQueries.list(owner)
			.stream()
			.filter(pet -> pet.id().equals(selectedId))
			.findFirst()
			.orElseThrow();
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(),
						state.round(), QuestOperation.DELIVER, "N043", null, null, List.of(reserve))));
		jdbc.update("UPDATE owned_creature SET party_position=100 WHERE id=?", Long.parseLong(selectedId));
		jdbc.update("UPDATE owned_creature SET locked=true WHERE id=?", Long.parseLong(selectedId));
		assertThrows(RuntimeException.class, () -> runtime.command(owner, delivery));
		jdbc.update("UPDATE owned_creature SET locked=false, level=level+1 WHERE id=?", Long.parseLong(selectedId));
		assertThrows(RuntimeException.class, () -> runtime.command(owner, delivery));
		jdbc.update("UPDATE owned_creature SET level=level-1 WHERE id=?", Long.parseLong(selectedId));
		assertEquals(gold, queries.find(owner).gold());
		// 在已选伙伴归档后的金币结算阶段失败，整个事务必须恢复伙伴、进度和请求号。
		jdbc.update("UPDATE player_character SET gold=? WHERE id=?", Long.MAX_VALUE, queries.requireId(owner));
		assertThrows(ArithmeticException.class, () -> runtime.command(owner, delivery));
		assertTrue(creatureQueries.list(owner).stream().anyMatch(pet -> pet.id().equals(selectedId)));
		assertNotEquals(QuestStatus.COMPLETED, runtime.latest(queries.requireId(owner), "N043").status());
		jdbc.update("UPDATE player_character SET gold=? WHERE id=?", gold, queries.requireId(owner));
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(gold + 60, queries.find(owner).gold());
		assertFalse(creatureQueries.list(owner).stream().anyMatch(pet -> pet.id().equals(selectedId)));
		assertTrue(creatureQueries.list(owner).stream().anyMatch(pet -> pet.id().equals(golems.getLast().id())));
		var different = new QuestInstanceRequest(delivery.requestId(), state.id(), state.round(),
				QuestOperation.DELIVER, "N043", null, null, List.of(golems.getLast()));
		assertThrows(RuntimeException.class, () -> runtime.command(owner, different));
	}

	@Test
	void fireArmoryConsumesOnlyRequiredPartsAndRewardsBlueprintOnce() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("U025", "S025", "F004", "F015"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", "SALAMANDER_CITY", queries.requireId(owner));
		for (String id : List.of("F056", "F057", "F058", "F059", "F060")) {
			var state = acceptOpening(owner, id);
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
			openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_SEALED_WORKS", "F_ARMORY_ENTRANCE");
			if (id.equals("F058"))
				transactions.run(() -> inventoryCommands.grant(owner, "ARMORY_PARTS", "F_ARMORY_PART", 8, false));
			for (var objective : catalog.step(id).objectives())
				if (objective.measure().name().equals("EVENT") && !objective.code().startsWith("VISIT:"))
					transactions.run(() -> facts.record(owner, objective.code()));
			openingMove(owner, "SALAMANDER_SEALED_WORKS", "SALAMANDER_ASH_ROAD", "SALAMANDER_CITY");
			var delivery = request(state, QuestOperation.DELIVER, id);
			runtime.command(owner, delivery);
			long gold = queries.find(owner).gold();
			runtime.command(owner, delivery);
			assertEquals(gold, queries.find(owner).gold());
		}
		assertEquals(2,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("F_ARMORY_PART"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("F_ARMORY_BLUEPRINT"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@Test
	void jointEpilogueAndChallengeRequireNewVisitsVictoriesAndCrafting() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("U025", "S025", "J025", "J026"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", "SALAMANDER_CITY", queries.requireId(owner));
		for (String id : List.of("J027", "J028", "J029", "J030", "E006", "E007", "E008", "E009", "E010")) {
			var state = acceptOpening(owner, id);
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
			openingMove(owner, "J_RESTORED_CROSSROADS");
			if (id.equals("J027"))
				assertThrows(RuntimeException.class, () -> openingMove(owner, "E_CHALLENGE_ENTRANCE"));
			if (id.equals("J028") || id.equals("E006") || id.equals("E008"))
				openingMove(owner, "E_CHALLENGE_ENTRANCE", "J_RESTORED_CROSSROADS");
			if (id.equals("J029"))
				openingMove(owner, "SQUARE", "J_RESTORED_CROSSROADS");
			for (var objective : catalog.step(id).objectives())
				if (!objective.code().startsWith("VISIT:"))
					transactions.run(() -> {
						for (int n = 0; n < objective.amount(); n++)
							facts.record(owner, objective.code());
					});
			openingMove(owner, "SALAMANDER_CITY");
			long xp = queries.find(owner).experience();
			var delivery = request(state, QuestOperation.DELIVER, id);
			assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
			long gold = queries.find(owner).gold();
			runtime.command(owner, delivery);
			assertEquals(xp, queries.find(owner).experience());
			assertEquals(gold, queries.find(owner).gold());
		}
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("E_FORGE_BLUEPRINT"))
					.mapToInt(item -> item.quantity())
					.sum());
		openingMove(owner, "J_RESTORED_CROSSROADS", "E_CHALLENGE_ENTRANCE");
	}

	@Test
	void jointRareCollectionAcceptsExistingPartnerAndKeepsIt() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("U025", "S025", "J025", "J026"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", "SALAMANDER_CITY", queries.requireId(owner));
		transactions.run(() -> {
			creatures.grant(owner, "ENDGAME_EXISTING_FOX", "SHADOWTAIL_FOX");
			inventoryCommands.grant(owner, "ENDGAME_SEALS", "SEAL_V", 3, false);
		});
		var pets = creatureQueries.list(owner).stream().map(pet -> pet.id()).toList();
		long xp = queries.find(owner).experience();
		for (String id : List.of("E011", "E012", "E013", "E014", "E015")) {
			var state = acceptOpening(owner, id);
			if (id.equals("E011") || id.equals("E013")) {
				assertThrows(RuntimeException.class,
						() -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
				openingMove(owner, "J_RESTORED_CROSSROADS", "E_RESIDUAL_FIELD");
				if (id.equals("E011"))
					assertThrows(RuntimeException.class, () -> openingMove(owner, "E_RARE_HABITAT"));
				else
					openingMove(owner, "E_RARE_HABITAT", "E_RESIDUAL_FIELD");
				openingMove(owner, "J_RESTORED_CROSSROADS", "SALAMANDER_CITY");
			}
			var delivery = request(state, QuestOperation.DELIVER, id);
			runtime.command(owner, delivery);
			runtime.command(owner, delivery);
		}
		assertEquals(xp, queries.find(owner).experience());
		assertEquals(pets, creatureQueries.list(owner).stream().map(pet -> pet.id()).toList());
		assertEquals(9,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("SEAL_V"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertFalse(inventoryQueries.list(owner).stream().anyMatch(item -> item.code().equals("E_COLLECTION_RECORD")));
		openingMove(owner, "J_RESTORED_CROSSROADS", "E_RESIDUAL_FIELD", "E_RARE_HABITAT");
	}

	@Test
	void jointRestoredRoadOpensRepeatableEndgameWithoutQuestExperience() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("U025", "S025", "J025"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", "SALAMANDER_CITY", queries.requireId(owner));
		var road = acceptOpening(owner, "J026");
		openingMove(owner, "J_RESTORED_CROSSROADS");
		assertThrows(RuntimeException.class, () -> openingMove(owner, "E_RESIDUAL_FIELD"));
		openingMove(owner, "SALAMANDER_CITY");
		runtime.command(owner, request(road, QuestOperation.DELIVER, "J026"));
		for (String id : List.of("E001", "E002", "E003", "E004", "E005")) {
			var state = acceptOpening(owner, id);
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
			openingMove(owner, "J_RESTORED_CROSSROADS", "E_RESIDUAL_FIELD");
			if (id.equals("E004"))
				transactions
					.run(() -> inventoryCommands.grant(owner, "ENDGAME_MATERIAL", "E_TEMPERED_FRAGMENT", 12, false));
			else
				for (var objective : catalog.step(id).objectives())
					if (!objective.code().startsWith("VISIT:"))
						transactions.run(() -> {
							for (int n = 0; n < objective.amount(); n++)
								facts.record(owner, objective.code());
						});
			openingMove(owner, "J_RESTORED_CROSSROADS", "SALAMANDER_CITY");
			long xp = queries.find(owner).experience();
			var delivery = request(state, QuestOperation.DELIVER, id);
			assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
			long gold = queries.find(owner).gold();
			runtime.command(owner, delivery);
			assertEquals(xp, queries.find(owner).experience());
			assertEquals(gold, queries.find(owner).gold());
		}
		assertEquals(2,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("E_TEMPERED_FRAGMENT"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertThrows(RuntimeException.class, () -> acceptOpening(owner, "E005"));
		openingMove(owner, "J_RESTORED_CROSSROADS", "E_RESIDUAL_FIELD");
	}

	@Test
	void jointEntranceRequiresPhysicalRecordsMaterialsAndNewGuardVictory() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("U025", "S025", "J005", "J010"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", "SALAMANDER_CITY", queries.requireId(owner));
		openingMove(owner, "J_CORE_PERIMETER");
		assertThrows(RuntimeException.class, () -> openingMove(owner, "J_ABYSS_ENTRANCE"));
		assertThrows(RuntimeException.class,
				() -> sceneCommands.execute(owner, "J011_RECOVER",
						new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
								UUID.randomUUID().toString(), queries.find(owner).revision(), null)));
		openingMove(owner, "SALAMANDER_CITY");
		var record = acceptOpening(owner, "J011");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(record, QuestOperation.DELIVER, "J011")));
		openingMove(owner, "J_CORE_PERIMETER");
		var action = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		var recovered = sceneCommands.execute(owner, "J011_RECOVER", action);
		assertEquals(recovered, sceneCommands.execute(owner, "J011_RECOVER", action));
		openingMove(owner, "SALAMANDER_CITY");
		runtime.command(owner, request(record, QuestOperation.DELIVER, "J011"));
		assertFalse(inventoryQueries.list(owner).stream().anyMatch(item -> item.code().equals("J_LOCATION_RECORD")));
		var materials = acceptOpening(owner, "J012");
		transactions.run(() -> inventoryCommands.grant(owner, "J_STABLE_PART", "LIFE_PART_3", 5, false));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(materials, QuestOperation.DELIVER, "J012")));
		assertEquals(5,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_PART_3"))
					.mapToInt(item -> item.quantity())
					.sum());
		transactions.run(() -> inventoryCommands.grant(owner, "J_STABLE_REAGENT", "LIFE_REAGENT_3", 5, false));
		var delivery = request(materials, QuestOperation.DELIVER, "J012");
		runtime.command(owner, delivery);
		runtime.command(owner, delivery);
		transactions.run(() -> facts.record(owner, "VICTORY:J_ENTRANCE_GUARD"));
		var guard = acceptOpening(owner, "J013");
		assertTrue(encounterAccess.allowed(owner, battleCatalog.encounter("J_ENTRANCE_GUARD")));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(guard, QuestOperation.DELIVER, "J013")));
		transactions.run(() -> facts.record(owner, "VICTORY:J_ENTRANCE_GUARD"));
		runtime.command(owner, request(guard, QuestOperation.DELIVER, "J013"));
		var unlock = acceptOpening(owner, "J014");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(unlock, QuestOperation.DELIVER, "J014")));
		openingMove(owner, "J_CORE_PERIMETER");
		action = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(UUID.randomUUID().toString(),
				queries.find(owner).revision(), null);
		var opened = sceneCommands.execute(owner, "J014_OPEN", action);
		assertEquals(opened, sceneCommands.execute(owner, "J014_OPEN", action));
		openingMove(owner, "SALAMANDER_CITY");
		runtime.command(owner, request(unlock, QuestOperation.DELIVER, "J014"));
		assertFalse(inventoryQueries.list(owner).stream().anyMatch(item -> item.code().equals("J_SEAL_PLAN")));
		var arrival = acceptOpening(owner, "J015");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(arrival, QuestOperation.DELIVER, "J015")));
		openingMove(owner, "J_CORE_PERIMETER", "J_ABYSS_ENTRANCE", "J_CORE_PERIMETER", "SALAMANDER_CITY");
		delivery = request(arrival, QuestOperation.DELIVER, "J015");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		long xp = queries.find(owner).experience(), gold = queries.find(owner).gold();
		runtime.command(owner, delivery);
		assertEquals(xp, queries.find(owner).experience());
		assertEquals(gold, queries.find(owner).gold());
		for (String code : List.of("LIFE_PART_3", "LIFE_REAGENT_3"))
			assertEquals(1,
					inventoryQueries.list(owner)
						.stream()
						.filter(item -> item.code().equals(code))
						.mapToInt(item -> item.quantity())
						.sum());
	}

	@Test
	void jointPerimeterRequiresFreshActionsAndKeepsManifest() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("U025", "S025", "J005"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", "SALAMANDER_CITY", queries.requireId(owner));
		for (String id : List.of("J006", "J007", "J008", "J009")) {
			for (var objective : catalog.step(id).objectives())
				transactions.run(() -> {
					for (int n = 0; n < objective.amount(); n++)
						facts.record(owner, objective.code());
				});
			var state = acceptOpening(owner, id);
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
			openingMove(owner, "J_CORE_PERIMETER");
			for (var objective : catalog.step(id).objectives()) {
				if (!objective.code().startsWith("VISIT:"))
					transactions.run(() -> {
						for (int n = 0; n < objective.amount(); n++)
							facts.record(owner, objective.code());
					});
			}
			openingMove(owner, "SALAMANDER_CITY");
			assertEquals(QuestStatus.COMPLETED,
					runtime.command(owner, request(state, QuestOperation.DELIVER, id)).instance().status());
		}
		var ready = acceptOpening(owner, "J010");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(ready, QuestOperation.DELIVER, "J010")));
		transactions.run(() -> inventoryCommands.grant(owner, "JOINT_MANIFEST", "J_READY_MANIFEST", 1, false));
		var delivery = request(ready, QuestOperation.DELIVER, "J010");
		runtime.command(owner, delivery);
		long xp = queries.find(owner).experience(), gold = queries.find(owner).gold();
		runtime.command(owner, delivery);
		assertEquals(xp, queries.find(owner).experience());
		assertEquals(gold, queries.find(owner).gold());
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("J_READY_MANIFEST"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@Test
	void jointSuppliesAllowEitherOrderAndConsumeAllPackagesExactlyOnce() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		for (var order : List.of(List.of("J001", "J002", "J003", "J004"), List.of("J004", "J003", "J002", "J001"))) {
			long owner = create();
			for (String gate : List.of("U025", "S025", "F025"))
				completeGate(owner, gate);
			jdbc.update("UPDATE player_character SET location=? WHERE id=?", "SALAMANDER_CITY",
					queries.requireId(owner));
			assertThrows(RuntimeException.class, () -> acceptOpening(owner, "J005"));
			for (String id : order) {
				var state = acceptOpening(owner, id);
				var materials = catalog.step(id)
					.effects()
					.stream()
					.filter(effect -> effect.kind().equals("TAKE_ITEM"))
					.toList();
				var first = materials.getFirst();
				transactions.run(() -> inventoryCommands.grant(owner, id + "_FIRST", first.reference(),
						Math.toIntExact(first.amount()), false));
				assertThrows(RuntimeException.class,
						() -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
				assertEquals(first.amount(),
						inventoryQueries.list(owner)
							.stream()
							.filter(item -> item.code().equals(first.reference()))
							.mapToInt(item -> item.quantity())
							.sum());
				var second = materials.getLast();
				transactions.run(() -> inventoryCommands.grant(owner, id + "_SECOND", second.reference(),
						Math.toIntExact(second.amount()), false));
				var delivery = request(state, QuestOperation.DELIVER, id);
				runtime.command(owner, delivery);
				long xp = queries.find(owner).experience(), gold = queries.find(owner).gold();
				runtime.command(owner, delivery);
				assertEquals(xp, queries.find(owner).experience());
				assertEquals(gold, queries.find(owner).gold());
				if (!id.equals(order.getLast()))
					assertThrows(RuntimeException.class, () -> acceptOpening(owner, "J005"));
			}
			var join = acceptOpening(owner, "J005");
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(join, QuestOperation.DELIVER, "J005")));
			for (String suffix : List.of("NOM", "UNDINE", "SYLPH", "SALAMANDER"))
				assertEquals(1,
						inventoryQueries.list(owner)
							.stream()
							.filter(item -> item.code().equals("J_SUPPLY_" + suffix))
							.mapToInt(item -> item.quantity())
							.sum());
			transactions.run(() -> inventoryCommands.grant(owner, "JOINT_DOSSIER", "F_JOINT_DOSSIER", 1, false));
			var delivery = request(join, QuestOperation.DELIVER, "J005");
			assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
			long xp = queries.find(owner).experience(), gold = queries.find(owner).gold();
			runtime.command(owner, delivery);
			assertEquals(xp, queries.find(owner).experience());
			assertEquals(gold, queries.find(owner).gold());
			assertFalse(inventoryQueries.list(owner)
				.stream()
				.anyMatch(item -> item.code().startsWith("J_SUPPLY_") || item.code().equals("F_JOINT_DOSSIER")));
			assertEquals(1,
					inventoryQueries.list(owner)
						.stream()
						.filter(item -> item.code().equals("J_READY_MANIFEST"))
						.mapToInt(item -> item.quantity())
						.sum());
		}
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "SAFE", "FIELD" })
	void fireSupportOrderSwitchesSourcesConsumesOnceAndOffersSafeCombat(String option) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("U025", "S025", "F001", "F015"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", "SALAMANDER_CITY", queries.requireId(owner));
		var survey = acceptOpening(owner, "F051");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(survey, QuestOperation.DELIVER, "F051")));
		sceneCommands.execute(owner, "F051_SURVEY",
				new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(UUID.randomUUID().toString(),
						queries.find(owner).revision(), null));
		runtime.command(owner, request(survey, QuestOperation.DELIVER, "F051"));
		var preparation = acceptOpening(owner, "F052");
		jdbc.update("INSERT INTO life_skill(character_id,skill,half_points) VALUES (?,?,?)", queries.requireId(owner),
				"MINING", 0);
		transactions.run(() -> inventoryCommands.grant(owner, "SUPPORT_TOOL", "TOOL_WOOD_PICKAXE", 1, false));
		runtime.command(owner, request(preparation, QuestOperation.DELIVER, "F052"));
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("PVP_TRUCE_TOKEN"))
					.mapToInt(item -> item.quantity())
					.sum());
		transactions.run(() -> {
			inventoryCommands.grant(owner, "SUPPORT_ORE", "F_SUPPORT_ORE", 8, false);
			facts.record(owner, "GATHER_NODE:F_SUPPORT_SAFE");
			facts.record(owner, "GATHER_NODE:F_SUPPORT_FIELD");
		});
		var source = acceptOpening(owner, "F053_SAFE");
		for (String choice : List.of("FIELD", "SAFE", option))
			runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(), source.id(), source.round(),
					QuestOperation.CHOOSE, null, "F_SUPPORT_SOURCE", choice));
		String step = "F053_" + option, node = option.equals("SAFE") ? "F_SUPPORT_SAFE" : "F_SUPPORT_FIELD";
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(source, QuestOperation.DELIVER, step)));
		transactions.run(() -> {
			for (int n = 0; n < 3; n++)
				facts.record(owner, "GATHER_NODE:" + node);
		});
		var delivery = request(source, QuestOperation.DELIVER, step);
		runtime.command(owner, delivery);
		runtime.command(owner, delivery);
		assertTrue(questQueries.done(owner, "F053"));
		assertEquals(2,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("F_SUPPORT_ORE"))
					.mapToInt(item -> item.quantity())
					.sum());
		var elite = acceptOpening(owner, "F054");
		assertEquals("SALAMANDER_CITY", battleCatalog.encounter("F_MINE_CONSTRUCT").location());
		transactions.run(() -> facts.record(owner, "VICTORY:F_MINE_CONSTRUCT"));
		runtime.command(owner, request(elite, QuestOperation.DELIVER, "F054"));
		var reward = acceptOpening(owner, "F055");
		delivery = request(reward, QuestOperation.DELIVER, "F055");
		runtime.command(owner, delivery);
		long gold = queries.find(owner).gold();
		runtime.command(owner, delivery);
		assertEquals(gold, queries.find(owner).gold());
		assertTrue(questQueries.done(owner, "F055"));
	}

	@Test
	void fireFurnaceRelicRequiresFreshBossesAndConsumesItsActualRelic() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("U025", "S025", "F001", "F004", "F012", "F015"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", "SALAMANDER_CITY", queries.requireId(owner));
		var survey = acceptOpening(owner, "F046");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(survey, QuestOperation.DELIVER, "F046")));
		openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_SEALED_WORKS", "SALAMANDER_ASH_ROAD", "SALAMANDER_CITY");
		transactions.run(() -> facts.record(owner, "CAPTURE_ENCOUNTER:F_CAPTURE_46"));
		runtime.command(owner, request(survey, QuestOperation.DELIVER, "F046"));
		var parts = acceptOpening(owner, "F047");
		transactions.run(() -> inventoryCommands.grant(owner, "F_RELIC_PARTS", "F_FURNACE_FRAGMENT", 7, false));
		var delivery = request(parts, QuestOperation.DELIVER, "F047");
		runtime.command(owner, delivery);
		runtime.command(owner, delivery);
		for (String code : List.of("F048", "F049")) {
			String boss = code.equals("F048") ? "F_FURNACE_GUARD" : "F_FURNACE_KEEPER";
			transactions.run(() -> facts.record(owner, "VICTORY:" + boss));
			var state = acceptOpening(owner, code);
			assertTrue(encounterAccess.allowed(owner, battleCatalog.encounter(boss)));
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(state, QuestOperation.DELIVER, code)));
			transactions.run(() -> facts.record(owner, "VICTORY:" + boss));
			runtime.command(owner, request(state, QuestOperation.DELIVER, code));
		}
		var reward = acceptOpening(owner, "F050");
		delivery = request(reward, QuestOperation.DELIVER, "F050");
		runtime.command(owner, delivery);
		long xp = queries.find(owner).experience(), gold = queries.find(owner).gold();
		runtime.command(owner, delivery);
		assertEquals(xp, queries.find(owner).experience());
		assertEquals(gold, queries.find(owner).gold());
		assertFalse(inventoryQueries.list(owner).stream().anyMatch(item -> item.code().equals("F_FURNACE_RELIC")));
		for (var expected : java.util.Map.of("F_FURNACE_FRAGMENT", 1, "F_FURNACE_BODY", 1).entrySet())
			assertEquals(expected.getValue().intValue(),
					inventoryQueries.list(owner)
						.stream()
						.filter(item -> item.code().equals(expected.getKey()))
						.mapToInt(item -> item.quantity())
						.sum());
	}

	@Test
	void fireManufacturingChecksCurrentSkillNewCraftAndKeepsCompleteEquipment() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("U025", "S025", "F005"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", "SALAMANDER_CITY", queries.requireId(owner));
		assertTrue(encounterAccess.allowed(owner, battleCatalog.encounter("F_RELIC_PATROL")));
		var skill = acceptOpening(owner, "F041");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(skill, QuestOperation.DELIVER, "F041")));
		long threshold = io.github.lishangbu.avalon.content.life.LifeSkillRules.threshold(7);
		jdbc.update("INSERT INTO life_skill(character_id,skill,half_points) VALUES (?,?,?)", queries.requireId(owner),
				"LEATHERWORK", threshold * 2 - 1);
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(skill, QuestOperation.DELIVER, "F041")));
		jdbc.update("UPDATE life_skill SET half_points=? WHERE character_id=? AND skill=?", threshold * 2,
				queries.requireId(owner), "LEATHERWORK");
		runtime.command(owner, request(skill, QuestOperation.DELIVER, "F041"));
		transactions.run(() -> {
			inventoryCommands.grant(owner, "F_FORGE_ORE", "LIFE_ORE_3", 14, false);
			inventoryCommands.grant(owner, "F_FORGE_HIDE", "F_HEAT_RESISTANT_HIDE", 7, false);
			inventoryCommands.grant(owner, "F_FORGE_BODY", "CRAFT_7_BODY", 1, true);
			facts.record(owner, "CRAFT_PRODUCT:CRAFT_7_BODY");
		});
		for (String code : List.of("F042", "F043")) {
			var state = acceptOpening(owner, code);
			var delivery = request(state, QuestOperation.DELIVER, code);
			runtime.command(owner, delivery);
			runtime.command(owner, delivery);
		}
		var craft = acceptOpening(owner, "F044");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(craft, QuestOperation.DELIVER, "F044")));
		transactions.run(() -> facts.record(owner, "CRAFT_PRODUCT:CRAFT_7_BODY"));
		runtime.command(owner, request(craft, QuestOperation.DELIVER, "F044"));
		var combat = acceptOpening(owner, "F045");
		transactions.run(() -> facts.record(owner, "VICTORY:F_RELIC_PATROL"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(combat, QuestOperation.DELIVER, "F045")));
		transactions.run(() -> facts.record(owner, "VICTORY_EQUIPPED_SLOT:BODY"));
		var delivery = request(combat, QuestOperation.DELIVER, "F045");
		runtime.command(owner, delivery);
		long xp = queries.find(owner).experience();
		runtime.command(owner, delivery);
		assertEquals(xp, queries.find(owner).experience());
		for (var expected : java.util.Map.of("LIFE_ORE_3", 2, "F_HEAT_RESISTANT_HIDE", 1, "CRAFT_7_BODY", 1).entrySet())
			assertEquals(expected.getValue().intValue(),
					inventoryQueries.list(owner)
						.stream()
						.filter(item -> item.code().equals(expected.getKey()))
						.mapToInt(item -> item.quantity())
						.sum());
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(ints = { 31, 36 })
	void fireCaptureChainsKeepExistingPartnersAndRequireNewDesignatedCaptures(int first) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("U025", "S025", "F001", "F004", "F005", "O003"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", "SALAMANDER_CITY", queries.requireId(owner));
		String species = first == 31 ? "EMBER_SPIRIT" : "SHADOWTAIL_FOX";
		transactions.run(() -> {
			creatures.grant(owner, "F_EXISTING_PARTNER", species);
			inventoryCommands.grant(owner, "F_ADVANCED_SEALS", "SEAL_IV", 3, false);
		});
		var pets = creatureQueries.list(owner).stream().map(pet -> pet.id()).toList();
		long xp = queries.find(owner).experience(), gold = queries.find(owner).gold();
		for (int n = first; n < first + 5; n++) {
			String code = "F0" + n, capture = "CAPTURE_ENCOUNTER:F_CAPTURE_" + n;
			transactions.run(() -> facts.record(owner, capture));
			var state = acceptOpening(owner, code);
			if (List.of(31, 32, 34, 36, 38, 39).contains(n)) {
				assertTrue(encounterAccess.allowed(owner, battleCatalog.encounter("F_CAPTURE_" + n)));
				transactions.run(() -> {
					facts.record(owner, "CAPTURE:" + species);
					facts.record(owner, "CAPTURE_ENCOUNTER:F_ASH_PATROL");
				});
				assertThrows(RuntimeException.class,
						() -> runtime.command(owner, request(state, QuestOperation.DELIVER, code)));
				transactions.run(() -> facts.record(owner, capture));
			}
			else if (n == 37) {
				assertTrue(encounterAccess.allowed(owner, battleCatalog.encounter("F_RELIC_PATROL")));
				transactions.run(() -> {
					for (int i = 0; i < 5; i++)
						facts.record(owner, "KILL_LOCATION:SALAMANDER_SEALED_WORKS");
					facts.record(owner, "KILL_LOCATION:SALAMANDER_ASH_ROAD");
				});
				assertThrows(RuntimeException.class,
						() -> runtime.command(owner, request(state, QuestOperation.DELIVER, code)));
				transactions.run(() -> facts.record(owner, "KILL_LOCATION:SALAMANDER_SEALED_WORKS"));
			}
			var delivery = request(state, QuestOperation.DELIVER, code);
			runtime.command(owner, delivery);
			runtime.command(owner, delivery);
		}
		assertEquals(xp + 5800, queries.find(owner).experience());
		assertEquals(gold + 450, queries.find(owner).gold());
		assertEquals(pets, creatureQueries.list(owner).stream().map(pet -> pet.id()).toList());
		assertEquals(3,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("SEAL_IV"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@Test
	void fireSupplyChainRequiresNewNodeAndCraftingAndConsumesOnlyDeliveries() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("U025", "S025", "F001", "F004"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", "SALAMANDER_CITY", queries.requireId(owner));
		transactions.run(() -> {
			inventoryCommands.grant(owner, "F_SILVER_STOCK", "LIFE_ORE_3", 12, false);
			facts.record(owner, "GATHER_NODE:F_VOLCANIC_SILVER");
			facts.record(owner, "CRAFT_PRODUCT:LIFE_PART_3");
		});
		var ore = acceptOpening(owner, "F026");
		var delivery = request(ore, QuestOperation.DELIVER, "F026");
		runtime.command(owner, delivery);
		runtime.command(owner, delivery);
		var tool = acceptOpening(owner, "F027");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(tool, QuestOperation.DELIVER, "F027")));
		transactions.run(() -> inventoryCommands.grant(owner, "F_SILVER_TOOL", "TOOL_SILVER_PICKAXE", 1, false));
		runtime.command(owner, request(tool, QuestOperation.DELIVER, "F027"));
		var gather = acceptOpening(owner, "F028");
		openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_CITY");
		transactions.run(() -> {
			facts.record(owner, "GATHER_NODE:F_REPAIR_ORE");
			for (int i = 0; i < 3; i++)
				facts.record(owner, "GATHER_NODE:F_VOLCANIC_SILVER");
		});
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(gather, QuestOperation.DELIVER, "F028")));
		transactions.run(() -> facts.record(owner, "GATHER_NODE:F_VOLCANIC_SILVER"));
		delivery = request(gather, QuestOperation.DELIVER, "F028");
		runtime.command(owner, delivery);
		runtime.command(owner, delivery);
		var craft = acceptOpening(owner, "F029");
		transactions.run(() -> {
			inventoryCommands.grant(owner, "F_SILVER_PARTS", "LIFE_PART_3", 3, false);
			facts.record(owner, "CRAFT_PRODUCT:LIFE_PART_3");
			facts.record(owner, "CRAFT_PRODUCT:LIFE_PART_2");
		});
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(craft, QuestOperation.DELIVER, "F029")));
		transactions.run(() -> facts.record(owner, "CRAFT_PRODUCT:LIFE_PART_3"));
		runtime.command(owner, request(craft, QuestOperation.DELIVER, "F029"));
		var supplies = acceptOpening(owner, "F030");
		delivery = request(supplies, QuestOperation.DELIVER, "F030");
		runtime.command(owner, delivery);
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		runtime.command(owner, delivery);
		assertEquals(xp, queries.find(owner).experience());
		assertEquals(gold, queries.find(owner).gold());
		for (var expected : java.util.Map.of("LIFE_ORE_3", 2, "LIFE_PART_3", 1, "TOOL_SILVER_PICKAXE", 1).entrySet())
			assertEquals(expected.getValue().intValue(),
					inventoryQueries.list(owner)
						.stream()
						.filter(item -> item.code().equals(expected.getKey()))
						.mapToInt(item -> item.quantity())
						.sum());
	}

	@Test
	void fireForecourtNeedsNewGatheringVictoriesAndEntranceOperation() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("U025", "S025", "F001", "F004", "F012", "F015"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", "SALAMANDER_CITY", queries.requireId(owner));
		transactions.run(() -> {
			inventoryCommands.grant(owner, "F_REPAIR_STOCK", "LIFE_ORE_0", 5, false);
			facts.record(owner, "GATHER_NODE:F_REPAIR_ORE");
			facts.record(owner, "VICTORY:F_VOLCANIC_ELITE");
		});
		var repair = acceptOpening(owner, "F016");
		transactions.run(() -> {
			facts.record(owner, "GATHER_NODE:S_SUPPLY_NODE");
			facts.record(owner, "GATHER_NODE:F_REPAIR_ORE");
			facts.record(owner, "GATHER_NODE:F_REPAIR_ORE");
		});
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(repair, QuestOperation.DELIVER, "F016")));
		transactions.run(() -> facts.record(owner, "GATHER_NODE:F_REPAIR_ORE"));
		var repairDelivery = request(repair, QuestOperation.DELIVER, "F016");
		runtime.command(owner, repairDelivery);
		runtime.command(owner, repairDelivery);
		assertEquals(2,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_ORE_0"))
					.mapToInt(item -> item.quantity())
					.sum());
		var elite = acceptOpening(owner, "F017");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(elite, QuestOperation.DELIVER, "F017")));
		openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_SEALED_WORKS", "F_CORE_APPROACH", "F_SEAL_FORECOURT");
		openingMove(owner, "F_CORE_APPROACH", "SALAMANDER_SEALED_WORKS", "SALAMANDER_ASH_ROAD", "SALAMANDER_CITY");
		transactions.run(() -> facts.record(owner, "VICTORY:F_VOLCANIC_ELITE"));
		runtime.command(owner, request(elite, QuestOperation.DELIVER, "F017"));
		var pack = acceptOpening(owner, "F018");
		transactions.run(() -> {
			facts.record(owner, "VICTORY:F_AFFECTED_PACK");
			facts.record(owner, "VICTORY:F_CORE_PATROL");
		});
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(pack, QuestOperation.DELIVER, "F018")));
		transactions.run(() -> facts.record(owner, "VICTORY:F_AFFECTED_PACK"));
		runtime.command(owner, request(pack, QuestOperation.DELIVER, "F018"));
		var entrance = acceptOpening(owner, "F019");
		transactions.run(() -> facts.record(owner, "VICTORY:F_UNDERGROUND_GUARD"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(entrance, QuestOperation.DELIVER, "F019")));
		assertThrows(RuntimeException.class,
				() -> sceneCommands.execute(owner, "F019_UNLOCK",
						new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
								UUID.randomUUID().toString(), queries.find(owner).revision(), null)));
		openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_SEALED_WORKS", "F_CORE_APPROACH", "F_SEAL_FORECOURT");
		var operation = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		var result = sceneCommands.execute(owner, "F019_UNLOCK", operation);
		assertEquals(result, sceneCommands.execute(owner, "F019_UNLOCK", operation));
		openingMove(owner, "F_CORE_APPROACH", "SALAMANDER_SEALED_WORKS", "SALAMANDER_ASH_ROAD", "SALAMANDER_CITY");
		var delivery = request(entrance, QuestOperation.DELIVER, "F019");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		long xp = queries.find(owner).experience();
		runtime.command(owner, delivery);
		assertEquals(xp, queries.find(owner).experience());
		var preparation = acceptOpening(owner, "F020");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(preparation, QuestOperation.DELIVER, "F020")));
		openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_SEALED_WORKS", "F_CORE_APPROACH", "F_SEAL_FORECOURT");
		openingMove(owner, "F_CORE_APPROACH", "SALAMANDER_SEALED_WORKS", "SALAMANDER_ASH_ROAD", "SALAMANDER_CITY");
		runtime.command(owner, request(preparation, QuestOperation.DELIVER, "F020"));
		openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_SEALED_WORKS", "F_CORE_APPROACH", "F_SEAL_FORECOURT");
		for (int index = 0; index < 3; index++) {
			String id = "F0" + (21 + index);
			String stage = "F_SEAL_" + List.of("OUTER", "CREATION", "CORE").get(index);
			transactions.run(() -> facts.record(owner, "VICTORY:" + stage));
			var objective = acceptOpening(owner, id);
			assertNotEquals(QuestStatus.COMPLETED,
					runtime.state(owner)
						.stream()
						.filter(value -> value.id().equals(objective.id()))
						.findFirst()
						.orElseThrow()
						.status());
			transactions.run(() -> facts.record(owner, "VICTORY:" + stage));
			assertTrue(questQueries.done(owner, id));
		}
		openingMove(owner, "F_CORE_APPROACH", "SALAMANDER_SEALED_WORKS", "SALAMANDER_ASH_ROAD", "SALAMANDER_CITY");
		var record = acceptOpening(owner, "F024");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(record, QuestOperation.DELIVER, "F024")));
		openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_SEALED_WORKS", "F_CORE_APPROACH", "F_SEAL_FORECOURT");
		var recovery = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		var recovered = sceneCommands.execute(owner, "F024_RECOVER", recovery);
		assertEquals(recovered, sceneCommands.execute(owner, "F024_RECOVER", recovery));
		openingMove(owner, "F_CORE_APPROACH", "SALAMANDER_SEALED_WORKS", "SALAMANDER_ASH_ROAD", "SALAMANDER_CITY");
		var recordDelivery = request(record, QuestOperation.DELIVER, "F024");
		runtime.command(owner, recordDelivery);
		runtime.command(owner, recordDelivery);
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("F_CORE_LOCATION")));
		var joint = acceptOpening(owner, "F025");
		var jointDelivery = request(joint, QuestOperation.DELIVER, "F025");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, jointDelivery).instance().status());
		xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		runtime.command(owner, jointDelivery);
		assertEquals(xp, queries.find(owner).experience());
		assertEquals(gold, queries.find(owner).gold());
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("F_JOINT_DOSSIER"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	/** 两城通路可在完成前换方法，公共奖励一次，随后必须现场启用机关。 */
	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.CsvSource({ "U,FIGHT", "U,REPAIR", "S,FIGHT", "S,REPAIR" })
	void middleDeepRouteSwitchesBeforeSettlementAndActivatesLocally(String prefix, String option) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		String city = prefix.equals("U") ? "UNDINE_CITY" : "SYLPH_CITY";
		String field = prefix.equals("U") ? "UNDINE_SHALLOWS" : "SYLPH_RIDGE_ROAD";
		String entry = prefix.equals("U") ? "UNDINE_ANCIENT_INLET" : "SYLPH_OLD_POST";
		for (String gate : List.of("N025", prefix + "004", prefix + "010"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", city, queries.requireId(owner));
		assertThrows(RuntimeException.class, () -> acceptOpening(owner, prefix + "012"));
		transactions.run(() -> {
			inventoryCommands.grant(owner, "DEEP_ROUTE_WOOD", "LIFE_WOOD_0", 10, false);
			facts.record(owner, "VICTORY:" + prefix + "_ROUTE_GUARD");
		});
		var state = acceptOpening(owner, prefix + "011_FIGHT");
		for (String selected : List.of(option.equals("FIGHT") ? "REPAIR" : "FIGHT", option))
			runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), state.round(),
					QuestOperation.CHOOSE, null, prefix + "_DEEP_ROUTE", selected));
		if (option.equals("FIGHT")) {
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(state, QuestOperation.DELIVER, prefix + "011_FIGHT")));
			transactions.run(() -> facts.record(owner, "VICTORY:" + prefix + "_ROUTE_GUARD"));
		}
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, prefix + "011_" + option);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(questQueries.done(owner, prefix + "011"));
		runtime.command(owner, delivery);
		assertEquals(xp + 1800, queries.find(owner).experience());
		assertEquals(gold + 60, queries.find(owner).gold());
		assertEquals(option.equals("FIGHT") ? 10 : 2,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_WOOD_0"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner,
						new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), state.round(),
								QuestOperation.CHOOSE, null, prefix + "_DEEP_ROUTE",
								option.equals("FIGHT") ? "REPAIR" : "FIGHT")));
		var activation = acceptOpening(owner, prefix + "012");
		assertThrows(RuntimeException.class,
				() -> sceneCommands.execute(owner, prefix + "012_ACTIVATE",
						new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
								UUID.randomUUID().toString(), queries.find(owner).revision(), null)));
		openingMove(owner, field, entry);
		var operation = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		var result = sceneCommands.execute(owner, prefix + "012_ACTIVATE", operation);
		assertEquals(result, sceneCommands.execute(owner, prefix + "012_ACTIVATE", operation));
		openingMove(owner, field, city);
		var activated = request(activation, QuestOperation.DELIVER, prefix + "012");
		runtime.command(owner, activated);
		runtime.command(owner, activated);
		assertEquals(xp + 3000, queries.find(owner).experience());
		assertEquals(gold + 120, queries.find(owner).gold());
		String deep = prefix + "_DEEP_APPROACH";
		transactions.run(() -> {
			for (int i = 0; i < 8; i++)
				facts.record(owner, "KILL_LOCATION:" + deep);
			facts.record(owner, "VICTORY:" + prefix + "_RESONANT_BEAST");
		});
		var patrol = acceptOpening(owner, prefix + "013");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(patrol, QuestOperation.DELIVER, prefix + "013")));
		transactions.run(() -> {
			for (int i = 0; i < 7; i++)
				facts.record(owner, "KILL_LOCATION:" + deep);
			facts.record(owner, "KILL_LOCATION:" + field);
		});
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(patrol, QuestOperation.DELIVER, prefix + "013")));
		transactions.run(() -> facts.record(owner, "KILL_LOCATION:" + deep));
		runtime.command(owner, request(patrol, QuestOperation.DELIVER, prefix + "013"));
		var beast = acceptOpening(owner, prefix + "014");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(beast, QuestOperation.DELIVER, prefix + "014")));
		transactions.run(() -> facts.record(owner, "VICTORY:" + prefix + "_RESONANT_BEAST"));
		runtime.command(owner, request(beast, QuestOperation.DELIVER, prefix + "014"));
		var shutdown = acceptOpening(owner, prefix + "015");
		assertThrows(RuntimeException.class,
				() -> sceneCommands.execute(owner, prefix + "015_SHUTDOWN",
						new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
								UUID.randomUUID().toString(), queries.find(owner).revision(), null)));
		openingMove(owner, field, entry, deep);
		var stopRequest = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		var stopped = sceneCommands.execute(owner, prefix + "015_SHUTDOWN", stopRequest);
		assertEquals(stopped, sceneCommands.execute(owner, prefix + "015_SHUTDOWN", stopRequest));
		openingMove(owner, entry, field, city);
		var shutdownDelivery = request(shutdown, QuestOperation.DELIVER, prefix + "015");
		runtime.command(owner, shutdownDelivery);
		runtime.command(owner, shutdownDelivery);
		assertEquals(xp + 8600, queries.find(owner).experience());
		assertEquals(gold + 330, queries.find(owner).gold());
		String forecourt = prefix + "_SEAL_FORECOURT";
		var arrival = acceptOpening(owner, prefix + "016");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(arrival, QuestOperation.DELIVER, prefix + "016")));
		openingMove(owner, field, entry, deep, forecourt, deep, entry, field, city);
		runtime.command(owner, request(arrival, QuestOperation.DELIVER, prefix + "016"));
		var materials = acceptOpening(owner, prefix + "017");
		transactions.run(() -> {
			inventoryCommands.grant(owner, "SEAL_ORE", "LIFE_ORE_0", 7, false);
			inventoryCommands.grant(owner, "SEAL_WOOD", "LIFE_WOOD_0", 4, false);
		});
		var inventoryBefore = inventoryQueries.list(owner);
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(materials, QuestOperation.DELIVER, prefix + "017")));
		assertEquals(inventoryBefore, inventoryQueries.list(owner));
		transactions.run(() -> inventoryCommands.grant(owner, "SEAL_LAST_ORE", "LIFE_ORE_0", 1, false));
		var materialDelivery = request(materials, QuestOperation.DELIVER, prefix + "017");
		runtime.command(owner, materialDelivery);
		var afterMaterials = inventoryQueries.list(owner);
		runtime.command(owner, materialDelivery);
		assertEquals(afterMaterials, inventoryQueries.list(owner));
		var spawn = acceptOpening(owner, prefix + "018");
		transactions.run(() -> facts.record(owner, "VICTORY:" + prefix + "_SEAL_SPAWN"));
		runtime.command(owner, request(spawn, QuestOperation.DELIVER, prefix + "018"));
		var unlock = acceptOpening(owner, prefix + "019");
		transactions.run(() -> facts.record(owner, "VICTORY:" + prefix + "_SEAL_ELITE"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(unlock, QuestOperation.DELIVER, prefix + "019")));
		openingMove(owner, field, entry, deep, forecourt);
		sceneCommands.execute(owner, prefix + "019_UNLOCK",
				new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(UUID.randomUUID().toString(),
						queries.find(owner).revision(), null));
		openingMove(owner, deep, entry, field, city);
		var unlocked = request(unlock, QuestOperation.DELIVER, prefix + "019");
		runtime.command(owner, unlocked);
		runtime.command(owner, unlocked);
		assertEquals(xp + 17000, queries.find(owner).experience());
		assertEquals(gold + 650, queries.find(owner).gold());
		var entrance = acceptOpening(owner, prefix + "020");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(entrance, QuestOperation.DELIVER, prefix + "020")));
		openingMove(owner, field, entry, deep, forecourt, deep, entry, field, city);
		runtime.command(owner, request(entrance, QuestOperation.DELIVER, prefix + "020"));
		openingMove(owner, field, entry, deep, forecourt);
		for (int n = 21; n <= 23; n++) {
			String code = prefix + "0" + n;
			acceptOpening(owner, code);
			String stage = prefix + "_SEAL" + List.of("_OUTER", "_CREATION", "_CORE").get(n - 21);
			transactions.run(() -> facts.record(owner, "VICTORY:" + stage));
			assertTrue(questQueries.done(owner, code));
		}
		openingMove(owner, deep, entry, field, city);
		var recordQuest = acceptOpening(owner, prefix + "024");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(recordQuest, QuestOperation.DELIVER, prefix + "024")));
		openingMove(owner, field, entry, deep, forecourt);
		var recoverRequest = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		var recovered = sceneCommands.execute(owner, prefix + "024_RECOVER", recoverRequest);
		assertEquals(recovered, sceneCommands.execute(owner, prefix + "024_RECOVER", recoverRequest));
		openingMove(owner, deep, entry, field, city);
		var recordDelivery = request(recordQuest, QuestOperation.DELIVER, prefix + "024");
		runtime.command(owner, recordDelivery);
		runtime.command(owner, recordDelivery);
		var resultQuest = acceptOpening(owner, prefix + "025");
		var resultDelivery = request(resultQuest, QuestOperation.DELIVER, prefix + "025");
		runtime.command(owner, resultDelivery);
		runtime.command(owner, resultDelivery);
		assertEquals(xp + 29600, queries.find(owner).experience());
		assertEquals(gold + 1130, queries.find(owner).gold());
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals(prefix + "_NODE_RECORD"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "U", "S" })
	void middleOptionalVaultRequiresKeyNewVictoryAndRecoveredRelic(String prefix) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		boolean water = prefix.equals("U");
		String city = water ? "UNDINE_CITY" : "SYLPH_CITY";
		String field = water ? "UNDINE_SHALLOWS" : "SYLPH_RIDGE_ROAD";
		String entry = water ? "UNDINE_ANCIENT_INLET" : "SYLPH_OLD_POST";
		String vault = prefix + "_RELIC_VAULT";
		int first = water ? 46 : 56;
		for (String gate : List.of("N025", prefix + "004", prefix + "007"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", city, queries.requireId(owner));
		long xp = queries.find(owner).experience(), gold = queries.find(owner).gold();
		for (int offset = 0; offset < 5; offset++) {
			String code = prefix + "0" + (first + offset);
			var state = acceptOpening(owner, code);
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(state, QuestOperation.DELIVER, code)));
			if (offset == 0)
				transactions.run(() -> facts.record(owner, "CAPTURE_ENCOUNTER:" + prefix + "_VAULT_FIELDWORK"));
			else if (offset == 1 || offset == 3) {
				String event = "VICTORY:" + (offset == 1 ? prefix + "_RELIC_GUARD" : vault + "_KEEPER");
				transactions.run(() -> facts.record(owner, event));
			}
			else {
				String action = vault + (offset == 2 ? "_KEY" : "_RELIC");
				openingMove(owner, field, entry, vault + "_ENTRANCE");
				var request = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
						UUID.randomUUID().toString(), queries.find(owner).revision(), null);
				var result = sceneCommands.execute(owner, action, request);
				assertEquals(result, sceneCommands.execute(owner, action, request));
				openingMove(owner, entry, field, city);
			}
			var delivery = request(state, QuestOperation.DELIVER, code);
			runtime.command(owner, delivery);
			runtime.command(owner, delivery);
		}
		assertEquals(xp + 6000, queries.find(owner).experience());
		assertEquals(gold + 400, queries.find(owner).gold());
		assertEquals(0,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().startsWith(vault))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals(water ? "CRAFT_4_BODY" : "S_HUNTER_BODY"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "WOOD", "ORE" })
	void undineBypassLocksSelectedMaterialsAndOpensGatheringSite(String option) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeGate(owner, "N025");
		jdbc.update("UPDATE player_character SET location='UNDINE_CITY' WHERE id=?", queries.requireId(owner));
		long xp = queries.find(owner).experience(), gold = queries.find(owner).gold();
		var survey = acceptOpening(owner, "U056");
		openingMove(owner, "UNDINE_SHALLOWS", "UNDINE_CITY");
		runtime.command(owner, request(survey, QuestOperation.DELIVER, "U056"));
		transactions.run(() -> {
			inventoryCommands.grant(owner, "BYPASS_WOOD", "LIFE_WOOD_0", 10, false);
			inventoryCommands.grant(owner, "BYPASS_ORE", "LIFE_ORE_0", 10, false);
		});
		var route = acceptOpening(owner, "U057_WOOD");
		runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(), route.id(), route.round(),
				QuestOperation.CHOOSE, null, "UNDINE_BYPASS", option));
		String other = option.equals("WOOD") ? "ORE" : "WOOD";
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(), route.id(),
						route.round(), QuestOperation.CHOOSE, null, "UNDINE_BYPASS", other)));
		var routeDelivery = request(route, QuestOperation.DELIVER, "U057_" + option);
		runtime.command(owner, routeDelivery);
		runtime.command(owner, routeDelivery);
		assertTrue(questQueries.done(owner, "U057"));
		for (String material : List.of("WOOD", "ORE"))
			assertEquals(material.equals(option) ? 2 : 10,
					inventoryQueries.list(owner)
						.stream()
						.filter(item -> item.code().equals("LIFE_" + material + "_0"))
						.mapToInt(item -> item.quantity())
						.sum());
		var patrol = acceptOpening(owner, "U058");
		transactions.run(() -> facts.record(owner, "VICTORY:U_APPROACH_PATROL"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(patrol, QuestOperation.DELIVER, "U058")));
		transactions.run(() -> facts.record(owner, "VICTORY:U_APPROACH_PATROL"));
		runtime.command(owner, request(patrol, QuestOperation.DELIVER, "U058"));
		var repair = acceptOpening(owner, "U059");
		openingMove(owner, "UNDINE_SHALLOWS");
		sceneCommands.execute(owner, "U059_REPAIR",
				new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(UUID.randomUUID().toString(),
						queries.find(owner).revision(), null));
		openingMove(owner, "UNDINE_CITY");
		runtime.command(owner, request(repair, QuestOperation.DELIVER, "U059"));
		var permit = acceptOpening(owner, "U060");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(permit, QuestOperation.DELIVER, "U060")));
		transactions.run(() -> inventoryCommands.grant(owner, "BYPASS_PARTS", "LIFE_PART_0", 3, false));
		var delivery = request(permit, QuestOperation.DELIVER, "U060");
		runtime.command(owner, delivery);
		runtime.command(owner, delivery);
		openingMove(owner, "UNDINE_SHALLOWS", "UNDINE_RESTORED_HERBS", "UNDINE_SHALLOWS", "UNDINE_CITY");
		assertEquals(xp + 5000, queries.find(owner).experience());
		assertEquals(gold + 350, queries.find(owner).gold());
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "U", "S" })
	void middleBountiesRequireNewDefeatsAndConsumeMarksOnce(String prefix) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		boolean water = prefix.equals("U");
		String city = water ? "UNDINE_CITY" : "SYLPH_CITY";
		String field = water ? "UNDINE_SHALLOWS" : "SYLPH_RIDGE_ROAD";
		completeGate(owner, "N025");
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", city, queries.requireId(owner));
		long xp = queries.find(owner).experience(), gold = queries.find(owner).gold();
		var kill = acceptOpening(owner, prefix + "051");
		transactions.run(() -> {
			for (int i = 0; i < 9; i++)
				facts.record(owner, "KILL_LOCATION:" + field);
			facts.record(owner, "KILL_LOCATION:FOREST_EDGE");
		});
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(kill, QuestOperation.DELIVER, prefix + "051")));
		transactions.run(() -> facts.record(owner, "KILL_LOCATION:" + field));
		runtime.command(owner, request(kill, QuestOperation.DELIVER, prefix + "051"));
		assertTrue(encounterAccess.allowed(owner, battleCatalog.encounter(prefix + "_BOUNTY_ELITE")));
		var material = acceptOpening(owner, prefix + "052");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(material, QuestOperation.DELIVER, prefix + "052")));
		transactions.run(() -> inventoryCommands.grant(owner, "BOUNTY_MATERIAL",
				water ? "UNDINE_SEAL_FRAGMENT" : "S_BOUNTY_MARK", water ? 5 : 2, false));
		var materialRequest = request(material, QuestOperation.DELIVER, prefix + "052");
		runtime.command(owner, materialRequest);
		runtime.command(owner, materialRequest);
		var visit = acceptOpening(owner, prefix + "053");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(visit, QuestOperation.DELIVER, prefix + "053")));
		openingMove(owner, field, city);
		runtime.command(owner, request(visit, QuestOperation.DELIVER, prefix + "053"));
		transactions.run(() -> facts.record(owner, "VICTORY:" + prefix + "_BOUNTY_ELITE"));
		var elite = acceptOpening(owner, prefix + "054");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(elite, QuestOperation.DELIVER, prefix + "054")));
		transactions.run(() -> facts.record(owner, "VICTORY:" + prefix + "_BOUNTY_ELITE"));
		runtime.command(owner, request(elite, QuestOperation.DELIVER, prefix + "054"));
		var reward = acceptOpening(owner, prefix + "055");
		if (water)
			transactions.run(() -> inventoryCommands.grant(owner, "BOUNTY_MARK", "U_BOUNTY_MARK", 2, false));
		var rewardRequest = request(reward, QuestOperation.DELIVER, prefix + "055");
		runtime.command(owner, rewardRequest);
		runtime.command(owner, rewardRequest);
		assertEquals(xp + 5000, queries.find(owner).experience());
		assertEquals(gold + 390, queries.find(owner).gold());
		assertEquals(water ? 1 : 0,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals(prefix + "_BOUNTY_MARK"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "U", "S" })
	void middleEquipmentOrdersConsumeMaterialsAndRewardOnce(String prefix) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		boolean water = prefix.equals("U");
		String city = water ? "UNDINE_CITY" : "SYLPH_CITY";
		for (String gate : List.of("N025", prefix + "004"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", city, queries.requireId(owner));
		long xp = queries.find(owner).experience(), gold = queries.find(owner).gold();
		for (int n = 41; n <= 45; n++) {
			String code = prefix + "0" + n;
			var state = acceptOpening(owner, code);
			if (water || n != 45)
				assertThrows(RuntimeException.class,
						() -> runtime.command(owner, request(state, QuestOperation.DELIVER, code)));
			if (water && n <= 43) {
				String material = n == 41 ? "LIFE_ORE_0" : n == 42 ? "LIFE_HIDE_0" : "LIFE_PART_0";
				int amount = n == 41 ? 8 : n == 42 ? 6 : 2;
				transactions.run(() -> inventoryCommands.grant(owner, code + "_MATERIAL", material, amount + 1, false));
				if (n == 43) {
					var before = inventoryQueries.list(owner);
					assertThrows(RuntimeException.class,
							() -> runtime.command(owner, request(state, QuestOperation.DELIVER, code)));
					assertEquals(before, inventoryQueries.list(owner));
					transactions.run(() -> inventoryCommands.grant(owner, code + "_WRAP", "LIFE_WRAP_0", 3, false));
				}
			}
			else if (water && n == 44) {
				transactions.run(() -> inventoryCommands.grant(owner, "ORDER_BODY", "CRAFT_4_BODY", 1, true));
				assertThrows(RuntimeException.class,
						() -> runtime.command(owner, request(state, QuestOperation.DELIVER, code)));
				transactions.run(() -> facts.record(owner, "CRAFT_PRODUCT:CRAFT_4_BODY"));
			}
			else if (water)
				transactions.run(() -> facts.record(owner, "VICTORY_EQUIPPED_SLOT:BODY"));
			else if (n == 41)
				openingMove(owner, "SYLPH_RIDGE_ROAD", "SYLPH_OLD_POST", "SYLPH_RIDGE_ROAD", city);
			else if (n == 42)
				transactions.run(() -> {
					for (int i = 0; i < 3; i++)
						facts.record(owner, "VICTORY:S_CAMP_PATROL");
				});
			else if (n == 43)
				transactions.run(() -> inventoryCommands.grant(owner, "CAMP_PARTS", "S_CAMP_FRAGMENT", 7, false));
			else if (n == 44) {
				assertTrue(encounterAccess.allowed(owner, battleCatalog.encounter("S_CAMP_KEEPER")));
				transactions.run(() -> facts.record(owner, "VICTORY:S_CAMP_KEEPER"));
			}
			// 045由044真实交付产生的遗物解锁，不另造任务物品。
			var delivery = request(state, QuestOperation.DELIVER, code);
			runtime.command(owner, delivery);
			var after = inventoryQueries.list(owner);
			runtime.command(owner, delivery);
			assertEquals(after, inventoryQueries.list(owner));
		}
		assertEquals(xp + 5000, queries.find(owner).experience());
		assertEquals(gold + 350, queries.find(owner).gold());
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals(water ? "CRAFT_4_BODY" : "S_HUNTER_BODY"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.CsvSource({ "U,31", "S,31", "U,36", "S,36" })
	void middleCaptureChainsSeparateNewCaptureFromOwnership(String prefix, int first) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		String city = prefix.equals("U") ? "UNDINE_CITY" : "SYLPH_CITY";
		for (String gate : List.of("N025", prefix + "004", prefix + "007"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", city, queries.requireId(owner));
		String displaySpecies = first == 31 ? (prefix.equals("U") ? "SHELL_TURTLE" : "SAND_HAWK")
				: (prefix.equals("U") ? "BLUEFIN_FISH" : "WHITE_CRANE");
		transactions.run(() -> {
			creatures.grant(owner, "CAPTURE_DISPLAY_GIFT", displaySpecies);
			inventoryCommands.grant(owner, "CAPTURE_SEALS", "SEAL_II", 3, false);
		});
		var petsBefore = creatureQueries.list(owner);
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		for (int n = first; n < first + 5; n++) {
			String code = prefix + "0" + n;
			String captureEvent = "CAPTURE_ENCOUNTER:" + prefix + "_CAPTURE_" + n;
			transactions.run(() -> facts.record(owner, captureEvent));
			var state = acceptOpening(owner, code);
			boolean capture = List.of(31, 32, 34, 36, 38, 39).contains(n) || prefix.equals("S") && n == 37;
			if (capture) {
				assertTrue(encounterAccess.allowed(owner, battleCatalog.encounter(prefix + "_CAPTURE_" + n)));
				transactions.run(() -> facts.record(owner, "CAPTURE:" + displaySpecies));
				assertThrows(RuntimeException.class,
						() -> runtime.command(owner, request(state, QuestOperation.DELIVER, code)));
				transactions.run(() -> facts.record(owner, captureEvent));
			}
			else if (prefix.equals("U") && n == 37) {
				transactions.run(() -> facts.record(owner, "VICTORY:U_RELIC_GUARD"));
				assertThrows(RuntimeException.class,
						() -> runtime.command(owner, request(state, QuestOperation.DELIVER, code)));
				transactions.run(() -> facts.record(owner, "VICTORY:U_RELIC_GUARD"));
			}
			var delivery = request(state, QuestOperation.DELIVER, code);
			runtime.command(owner, delivery);
			runtime.command(owner, delivery);
		}
		assertEquals(xp + (prefix.equals("S") && first == 36 ? 4100 : 3700), queries.find(owner).experience());
		assertEquals(gold + 300, queries.find(owner).gold());
		assertEquals(petsBefore.stream().map(pet -> pet.id()).toList(),
				creatureQueries.list(owner).stream().map(pet -> pet.id()).toList());
		assertEquals(3,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("SEAL_II"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "U", "S" })
	void middleSupplyChainRequiresLocalSuccessAndNewCraft(String prefix) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		String city = prefix.equals("U") ? "UNDINE_CITY" : "SYLPH_CITY";
		String field = prefix.equals("U") ? "UNDINE_SHALLOWS" : "SYLPH_RIDGE_ROAD";
		String tool = prefix.equals("U") ? "TOOL_WOOD_PINCERS" : "TOOL_WOOD_PICKAXE";
		String resource = prefix.equals("U") ? "LIFE_HERB_0" : "LIFE_ORE_0";
		String product = prefix.equals("U") ? "LIFE_REAGENT_0" : "LIFE_PART_0";
		completeGate(owner, "N025");
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", city, queries.requireId(owner));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		var arrival = acceptOpening(owner, prefix + "026");
		openingMove(owner, field, city);
		runtime.command(owner, request(arrival, QuestOperation.DELIVER, prefix + "026"));
		var preparation = acceptOpening(owner, prefix + "027");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(preparation, QuestOperation.DELIVER, prefix + "027")));
		transactions.run(() -> inventoryCommands.grant(owner, "SUPPLY_TOOL", tool, 1, false));
		runtime.command(owner, request(preparation, QuestOperation.DELIVER, prefix + "027"));
		String nodeEvent = "GATHER_NODE:" + prefix + "_SUPPLY_NODE";
		transactions.run(() -> facts.record(owner, nodeEvent));
		var gathering = acceptOpening(owner, prefix + "028");
		transactions.run(() -> {
			facts.record(owner, nodeEvent);
			facts.record(owner, nodeEvent);
			facts.record(owner, "GATHER_NODE:" + (prefix.equals("U") ? "S" : "U") + "_SUPPLY_NODE");
			facts.record(owner, "GATHER_SUCCESS:MINING");
		});
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(gathering, QuestOperation.DELIVER, prefix + "028")));
		transactions.run(() -> facts.record(owner, nodeEvent));
		runtime.command(owner, request(gathering, QuestOperation.DELIVER, prefix + "028"));
		transactions.run(() -> {
			inventoryCommands.grant(owner, "SUPPLY_PRODUCT", product, 2, false);
			facts.record(owner, "CRAFT_PRODUCT:" + product);
		});
		var craft = acceptOpening(owner, prefix + "029");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(craft, QuestOperation.DELIVER, prefix + "029")));
		transactions.run(() -> facts.record(owner, "CRAFT_PRODUCT:" + product));
		runtime.command(owner, request(craft, QuestOperation.DELIVER, prefix + "029"));
		var delivery = acceptOpening(owner, prefix + "030");
		transactions.run(() -> inventoryCommands.grant(owner, "SUPPLY_RESOURCE", resource, 5, false));
		var before = inventoryQueries.list(owner);
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(delivery, QuestOperation.DELIVER, prefix + "030")));
		assertEquals(before, inventoryQueries.list(owner));
		transactions.run(() -> inventoryCommands.grant(owner, "SUPPLY_LAST_RESOURCE", resource, 2, false));
		var delivered = request(delivery, QuestOperation.DELIVER, prefix + "030");
		runtime.command(owner, delivered);
		runtime.command(owner, delivered);
		assertEquals(xp + 3000, queries.find(owner).experience());
		assertEquals(gold + 250, queries.find(owner).gold());
		for (String code : List.of(tool, product, resource))
			assertEquals(1,
					inventoryQueries.list(owner)
						.stream()
						.filter(item -> item.code().equals(code))
						.mapToInt(item -> item.quantity())
						.sum());
	}

	/** 两城第二短链使用正式目录，验证准备消费与守卫事件窗口；不替代自然战斗。 */
	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "U", "S" })
	void middleRelicPreparationsRequireActualVisitAndAtomicMaterials(String prefix) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		String city = prefix.equals("U") ? "UNDINE_CITY" : "SYLPH_CITY";
		String field = prefix.equals("U") ? "UNDINE_SHALLOWS" : "SYLPH_RIDGE_ROAD";
		String entry = prefix.equals("U") ? "UNDINE_ANCIENT_INLET" : "SYLPH_OLD_POST";
		for (String gate : List.of("N025", prefix + "004", prefix + "005"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", entry, queries.requireId(owner));
		transactions.run(() -> facts.record(owner, "VISIT:" + entry));
		openingMove(owner, field, city);
		var preparation = acceptOpening(owner, prefix + "006");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(preparation, QuestOperation.DELIVER, prefix + "006")));
		if (prefix.equals("U")) {
			transactions.run(() -> inventoryCommands.grant(owner, "RELIC_TOOL", "TOOL_WOOD_HAMMER", 1, false));
		}
		else {
			openingMove(owner, field, entry, field, city);
		}
		var preparationDelivery = request(preparation, QuestOperation.DELIVER, prefix + "006");
		runtime.command(owner, preparationDelivery);
		runtime.command(owner, preparationDelivery);
		if (prefix.equals("U"))
			assertEquals(1,
					inventoryQueries.list(owner)
						.stream()
						.filter(item -> item.code().equals("TOOL_WOOD_HAMMER"))
						.mapToInt(item -> item.quantity())
						.sum());
		var material = acceptOpening(owner, prefix + "007");
		assertFalse(encounterAccess.allowed(owner, battleCatalog.encounter(prefix + "_RELIC_GUARD")));
		transactions.run(() -> {
			inventoryCommands.grant(owner, "RELIC_WOOD", "LIFE_WOOD_0", 8, false);
			inventoryCommands.grant(owner, "RELIC_ORE", "LIFE_ORE_0", 5, false);
		});
		var before = inventoryQueries.list(owner);
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(material, QuestOperation.DELIVER, prefix + "007")));
		assertEquals(before, inventoryQueries.list(owner));
		transactions.run(() -> inventoryCommands.grant(owner, "RELIC_ORE_LAST", "LIFE_ORE_0", 2, false));
		var materialDelivery = request(material, QuestOperation.DELIVER, prefix + "007");
		runtime.command(owner, materialDelivery);
		var remaining = inventoryQueries.list(owner);
		runtime.command(owner, materialDelivery);
		assertEquals(remaining, inventoryQueries.list(owner));
		assertEquals(2,
				remaining.stream()
					.filter(item -> item.code().equals("LIFE_WOOD_0"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(1,
				remaining.stream()
					.filter(item -> item.code().equals("LIFE_ORE_0"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertTrue(encounterAccess.allowed(owner, battleCatalog.encounter(prefix + "_RELIC_GUARD")));
		transactions.run(() -> facts.record(owner, "VICTORY:" + prefix + "_RELIC_GUARD"));
		var guard = acceptOpening(owner, prefix + "008");
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(guard, QuestOperation.DELIVER, prefix + "008")));
		transactions.run(() -> facts.record(owner, "VICTORY:" + prefix + "_RELIC_GUARD"));
		var guardDelivery = request(guard, QuestOperation.DELIVER, prefix + "008");
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, guardDelivery).instance().status());
		runtime.command(owner, guardDelivery);
		assertEquals(xp + 2200, queries.find(owner).experience());
		assertEquals(gold + 60, queries.find(owner).gold());
		assertNotNull(acceptOpening(owner, prefix + "009"));
	}

	/** 两城正式回收目标必须在接取和守卫前置满足后现场取物，交付只扣一份。 */
	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.CsvSource({
			"U009,U008,UNDINE_CITY,UNDINE_ANCIENT_INLET,U009_RECOVER,U_RECOVERED_DEVICE",
			"U010,U009,UNDINE_CITY,UNDINE_ANCIENT_INLET,U010_IMPRINT,U_NODE_RECORD",
			"S009,S008,SYLPH_CITY,SYLPH_OLD_POST,S009_RECOVER,S_RECOVERED_DEVICE",
			"S010,S009,SYLPH_CITY,SYLPH_OLD_POST,S010_IMPRINT,S_NODE_RECORD" })
	void middleRelicRecoveryRequiresLocalActionAndConsumesEvidenceOnce(String quest, String gate, String city,
			String location, String action, String item) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeGate(owner, "N025");
		completeGate(owner, quest.substring(0, 1) + "004");
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", location, queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> sceneCommands.execute(owner, action,
						new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
								UUID.randomUUID().toString(), queries.find(owner).revision(), null)));
		completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", city, queries.requireId(owner));
		var state = acceptOpening(owner, quest);
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, quest)));
		assertThrows(RuntimeException.class,
				() -> sceneCommands.execute(owner, action,
						new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
								UUID.randomUUID().toString(), queries.find(owner).revision(), null)));
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", location, queries.requireId(owner));
		var recover = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		var result = sceneCommands.execute(owner, action, recover);
		assertEquals(1, result.quantity());
		assertEquals(result, sceneCommands.execute(owner, action, recover));
		assertEquals(0,
				sceneCommands
					.execute(owner, action,
							new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
									UUID.randomUUID().toString(), queries.find(owner).revision(), null))
					.quantity());
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", city, queries.requireId(owner));
		long xp = queries.find(owner).experience();
		var delivery = request(state, QuestOperation.DELIVER, quest);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(xp + 1200, queries.find(owner).experience());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(value -> value.code().equals(item)));
	}

	/** 两城任选先后：真实移动与开门，隔离事件验证并行准备、消费与首次奖励。 */
	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(booleans = { true, false })
	void middleCityApproachesWorkInEitherOrderWithoutLegacyFacts(boolean waterFirst) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "N025"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		for (String prefix : waterFirst ? List.of("U", "S") : List.of("S", "U")) {
			boolean water = prefix.equals("U");
			String city = water ? "UNDINE_CITY" : "SYLPH_CITY";
			String field = water ? "UNDINE_SHALLOWS" : "SYLPH_RIDGE_ROAD";
			String entry = water ? "UNDINE_ANCIENT_INLET" : "SYLPH_OLD_POST";
			String material = water ? "UNDINE_SEAL_FRAGMENT" : "SYLPH_ROAD_BRACE";
			openingMove(owner, water ? "GNOME_PORT_ROAD" : "SYLPH_PASS", city);
			openingBattles.recover(owner);
			transactions.run(() -> facts.record(owner, "VICTORY:" + prefix + "_APPROACH_PATROL"));
			var first = acceptOpening(owner, prefix + "001");
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(first, QuestOperation.DELIVER, prefix + "001")));
			transactions.run(() -> facts.record(owner, "VICTORY:" + prefix + "_APPROACH_PATROL"));
			runtime.command(owner, request(first, QuestOperation.DELIVER, prefix + "001"));
			var clear = acceptOpening(owner, prefix + "002");
			var supplies = acceptOpening(owner, prefix + "003");
			assertThrows(RuntimeException.class, () -> acceptOpening(owner, prefix + "004"));
			transactions.run(() -> inventoryCommands.grant(owner, prefix + "_PARTS", material, 3, false));
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(supplies, QuestOperation.DELIVER, prefix + "003")));
			transactions.run(() -> inventoryCommands.grant(owner, prefix + "_LAST_PARTS", material, 2, false));
			var supplyDelivery = request(supplies, QuestOperation.DELIVER, prefix + "003");
			runtime.command(owner, supplyDelivery);
			runtime.command(owner, supplyDelivery);
			assertEquals(1,
					inventoryQueries.list(owner)
						.stream()
						.filter(item -> item.code().equals(material))
						.mapToInt(item -> item.quantity())
						.sum());
			assertThrows(RuntimeException.class, () -> acceptOpening(owner, prefix + "004"));
			transactions.run(() -> {
				for (int i = 0; i < 7; i++)
					facts.record(owner, "KILL_LOCATION:" + field);
				facts.record(owner, "KILL_LOCATION:FOREST_EDGE");
			});
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(clear, QuestOperation.DELIVER, prefix + "002")));
			transactions.run(() -> facts.record(owner, "KILL_LOCATION:" + field));
			runtime.command(owner, request(clear, QuestOperation.DELIVER, prefix + "002"));
			transactions.run(() -> facts.record(owner, "VICTORY:" + prefix + "_APPROACH_BOSS"));
			var boss = acceptOpening(owner, prefix + "004");
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(boss, QuestOperation.DELIVER, prefix + "004")));
			openingMove(owner, field);
			assertThrows(RuntimeException.class, () -> characters.move(owner, entry, queries.find(owner).revision()));
			transactions.run(() -> facts.record(owner, "VICTORY:" + prefix + "_APPROACH_BOSS"));
			openingMove(owner, city);
			var bossDelivery = request(boss, QuestOperation.DELIVER, prefix + "004");
			runtime.command(owner, bossDelivery);
			var gear = inventoryQueries.list(owner);
			runtime.command(owner, bossDelivery);
			assertEquals(gear, inventoryQueries.list(owner));
			assertEquals(1, gear.stream().filter(item -> item.code().equals(prefix + "_APPROACH_BODY")).count());
			var enter = acceptOpening(owner, prefix + "005");
			openingMove(owner, field, entry);
			openingMove(owner, field, city);
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(enter, QuestOperation.DELIVER, prefix + "005")));
			openingMove(owner, field, entry);
			var operation = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
					UUID.randomUUID().toString(), queries.find(owner).revision(), null);
			var opened = sceneCommands.execute(owner, prefix + "005_OPEN", operation);
			assertEquals(opened, sceneCommands.execute(owner, prefix + "005_OPEN", operation));
			openingMove(owner, field, city);
			var delivery = request(enter, QuestOperation.DELIVER, prefix + "005");
			long xp = queries.find(owner).experience();
			assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
			runtime.command(owner, delivery);
			assertEquals(xp + 1200, queries.find(owner).experience());
			openingMove(owner, water ? "GNOME_PORT_ROAD" : "SYLPH_PASS", "CITY_OUTSKIRTS");
		}
		assertTrue(questQueries.done(owner, "U005"));
		assertTrue(questQueries.done(owner, "S005"));
	}

	/** 区域悬赏排除历史和异地事件；七只不能交付，第八只完成且报酬不重复。 */
	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.CsvSource({ "N046,KILL_LOCATION:CITY_OUTSKIRTS",
			"N057,KILL_LOCATION:GNOME_MINE_GATE" })
	void regionalBountiesRequireEightNewDefeatsInTheirOwnArea(String quest, String event) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O015", "N007", "N056"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> {
			for (int i = 0; i < 8; i++)
				facts.record(owner, event);
		});
		var state = accept(owner, quest);
		transactions.run(() -> {
			for (int i = 0; i < 8; i++) {
				facts.record(owner, "KILL:CLAW_CAT");
				facts.record(owner, "KILL_LOCATION:FOREST_EDGE");
			}
			for (int i = 0; i < 7; i++)
				facts.record(owner, event);
		});
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, quest)));
		transactions.run(() -> facts.record(owner, event));
		var delivery = request(state, QuestOperation.DELIVER, quest);
		long gold = queries.find(owner).gold();
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(gold + 30, queries.find(owner).gold());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 正式捕捉任务排除历史和其他来源，封印球奖励只结算一次。 */
	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.CsvSource({ "N036,CAPTURE_WILD_LOCATION:PLAIN_SIGN",
			"N041,CAPTURE_ENCOUNTER:NOM_CAVERN_GOLEM", "N050,CAPTURE_ENCOUNTER:NOM_TOWER_SPROUT" })
	void regionalCaptureRequiresNewActionAndAwardsBallsOnce(String quest, String event) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O015", "N007"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_PETS' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> facts.record(owner, event));
		var state = accept(owner, quest);
		transactions.run(() -> {
			facts.record(owner, "CAPTURE:STONE_GOLEM");
			facts.record(owner, "CAPTURE:CLAW_CAT");
			facts.record(owner, "CAPTURE_WILD_LOCATION:FOREST_EDGE");
		});
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, quest)));
		var before = inventoryQueries.list(owner)
			.stream()
			.filter(item -> item.code().equals("SEAL_I"))
			.mapToInt(item -> item.quantity())
			.sum();
		transactions.run(() -> facts.record(owner, event));
		var delivery = request(state, QuestOperation.DELIVER, quest);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(before + 3,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("SEAL_I"))
					.mapToInt(item -> item.quantity())
					.sum());
		var settled = inventoryQueries.list(owner);
		runtime.command(owner, delivery);
		assertEquals(settled, inventoryQueries.list(owner));
	}

	/** 两个共振源各需新胜利，重复同一处不能替代，结算后才开放普通伙伴遭遇。 */
	@Test
	void resonanceSourcesRequireBothDistinctVictoriesBeforeRestoredBeastsAppear() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeOpening(owner);
		completeGate(owner, "N016");
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> {
			facts.record(owner, "VICTORY:NOM_RESONANCE_NORTH");
			facts.record(owner, "VICTORY:NOM_RESONANCE_SOUTH");
		});
		var state = accept(owner, "N017");
		var restored = battleCatalog.encounter("NOM_RESTORED_BEASTS");
		assertFalse(encounterAccess.allowed(owner, restored));
		transactions.run(() -> {
			facts.record(owner, "KILL:STONE_GOLEM");
			facts.record(owner, "VICTORY:NOM_RESONANCE_NORTH");
			facts.record(owner, "VICTORY:NOM_RESONANCE_NORTH");
		});
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N017")));
		assertFalse(encounterAccess.allowed(owner, restored));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_RESONANCE_SOUTH"));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N017");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(encounterAccess.allowed(owner, restored));
		runtime.command(owner, delivery);
		assertEquals(xp + 116000, queries.find(owner).experience());
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 物资可先于异常战斗完成，库存有效；两项都完成才开放后续突破任务。 */
	@Test
	void sealingSuppliesCanFinishInParallelAndBothTasksGateTheNextQuest() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeOpening(owner);
		completeGate(owner, "N016");
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> inventoryCommands.grant(owner, "SEAL_ORE", "LIFE_ORE_0", 7, false));
		var state = accept(owner, "N018");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N018")));
		transactions.run(() -> inventoryCommands.grant(owner, "SEAL_ORE_LAST", "LIFE_ORE_0", 1, false));
		long xp = queries.find(owner).experience();
		var delivery = request(state, QuestOperation.DELIVER, "N018");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertFalse(questQueries.done(owner, "N017"));
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("LIFE_ORE_0")));
		runtime.command(owner, delivery);
		assertEquals(xp + 138000, queries.find(owner).experience());
		assertThrows(RuntimeException.class, () -> accept(owner, "N019_OUTER"));
		completeGate(owner, "N017");
		assertNotNull(accept(owner, "N019_OUTER"));
	}

	/** 两道守卫按顺序开放，中途离开不会丢外庭进度，装备及经验只结算一次。 */
	@Test
	void ruinGuardsOpenSequentiallyAndPreserveProgressAcrossReturnTrips() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "N016", "N017", "N018"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N019_OUTER");
		var inner = battleCatalog.encounter("NOM_RUIN_INNER_GUARD");
		assertFalse(encounterAccess.allowed(owner, inner));
		jdbc.update("UPDATE player_character SET location='NOM_RESONANCE_GALLERY' WHERE id=?",
				queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> characters.move(owner, "NOM_RUIN_INNER_COURT", queries.find(owner).revision()));
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		assertFalse(questQueries.done(owner, "N019_OUTER"));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_RUIN_OUTER_GUARD"));
		assertTrue(questQueries.done(owner, "N019_OUTER"));
		assertTrue(encounterAccess.allowed(owner, inner));
		characters.move(owner, "NOM_RUIN_INNER_COURT", queries.find(owner).revision());
		characters.move(owner, "NOM_RESONANCE_GALLERY", queries.find(owner).revision());
		characters.move(owner, "NOM_RUIN_INNER_COURT", queries.find(owner).revision());
		assertTrue(questQueries.done(owner, "N019_OUTER"));
		assertFalse(questQueries.done(owner, "N019"));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_RUIN_INNER_GUARD"));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N019");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		var inventory = inventoryQueries.list(owner);
		assertEquals(1,
				inventory.stream()
					.filter(item -> item.code().equals("NOM_RUIN_BOOTS"))
					.mapToInt(item -> item.quantity())
					.sum());
		runtime.command(owner, delivery);
		assertEquals(inventory, inventoryQueries.list(owner));
		assertEquals(xp + 162000, queries.find(owner).experience());
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 四职业从新角色实际行走完成开篇准备，不注入资产或剧情事实。 */
	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "WARRIOR", "RANGER", "MAGE", "PRIEST" })
	void rewrittenOpeningRegistersPartnerAndActualThreePieceLoadout(String profession) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		String name = "op" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		registration.register(name, "Quest-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, profession);
		characters.move(owner, "STONE_PATH", queries.find(owner).revision());
		characters.move(owner, "SQUARE", queries.find(owner).revision());
		var visit = acceptOpening(owner, "O001");
		characters.move(owner, "PET_CENTER", queries.find(owner).revision());
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(visit, QuestOperation.DELIVER, "O001")).instance().status());
		var received = acceptOpening(owner, "O002_RECEIVE");
		var receive = request(received, QuestOperation.DELIVER, "O002_RECEIVE");
		runtime.command(owner, receive);
		runtime.command(owner, receive);
		assertEquals(1, creatureQueries.list(owner).size());
		var original = creatureQueries.list(owner).getFirst();
		var party = acceptOpening(owner, "O002");
		creatures.position(owner, Long.parseLong(original.id()), 2);
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(party, QuestOperation.DELIVER, "O002")));
		// 同种替代伙伴不能代替该任务实际赠予的个体。
		transactions.run(() -> creatures.grant(owner, "OTHER_CAT", "CLAW_CAT"));
		var other = creatureQueries.list(owner)
			.stream()
			.filter(pet -> !pet.id().equals(original.id()))
			.findFirst()
			.orElseThrow();
		creatures.position(owner, Long.parseLong(other.id()), 0);
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(party, QuestOperation.DELIVER, "O002")));
		creatures.position(owner, Long.parseLong(original.id()), 0);
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(party, QuestOperation.DELIVER, "O002")).instance().status());
		characters.move(owner, "SQUARE", queries.find(owner).revision());
		characters.move(owner, "WEAPON_SHOP", queries.find(owner).revision());
		var gear = acceptOpening(owner, "O003_RECEIVE");
		var receiveGear = request(gear, QuestOperation.DELIVER, "O003_RECEIVE");
		runtime.command(owner, receiveGear);
		var granted = inventoryQueries.list(owner);
		runtime.command(owner, receiveGear);
		assertEquals(granted, inventoryQueries.list(owner));
		var equipped = acceptOpening(owner, "O003");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(equipped, QuestOperation.DELIVER, "O003")));
		for (var item : inventoryQueries.list(owner))
			if (item.slot() != null)
				inventoryCommands.equip(owner, Long.parseLong(item.id()));
		var boots = inventoryQueries.list(owner)
			.stream()
			.filter(item -> item.code().equals("STARTER_FEET"))
			.findFirst()
			.orElseThrow();
		inventoryCommands.unequip(owner, Long.parseLong(boots.id()));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(equipped, QuestOperation.DELIVER, "O003")));
		inventoryCommands.equip(owner, Long.parseLong(boots.id()));
		var delivery = request(equipped, QuestOperation.DELIVER, "O003");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		var completedGear = inventoryQueries.list(owner);
		long xp = queries.find(owner).experience();
		runtime.command(owner, delivery);
		assertEquals(xp, queries.find(owner).experience());
		assertEquals(completedGear, inventoryQueries.list(owner));
		characters.move(owner, "SQUARE", queries.find(owner).revision());
		characters.move(owner, "PET_CENTER", queries.find(owner).revision());
		openingBattles.recover(owner);
		var trial = acceptOpening(owner, "O004");
		openingRiversideTrip(owner);
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(trial, QuestOperation.DELIVER, "O004")).instance().status());
		var herbs = acceptOpening(owner, "O005");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(herbs, QuestOperation.DELIVER, "O005")));
		openingRiversideTrip(owner);
		openingRiversideTrip(owner);
		var herbDelivery = request(herbs, QuestOperation.DELIVER, "O005");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, herbDelivery).instance().status());
		runtime.command(owner, herbDelivery);
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("LIFE_HERB_0")));
		openingMove(owner, "SQUARE", "STONE_PATH", "RIVERSIDE", "FOREST_EDGE");
		var forest = acceptOpening(owner, "O006");
		for (int i = 0; i < 4; i++) {
			openingFight(owner, "OPENING_FOREST_CLEAR");
			if (i < 3) {
				assertThrows(RuntimeException.class,
						() -> runtime.command(owner, request(forest, QuestOperation.DELIVER, "O006")));
				openingMove(owner, "RIVERSIDE", "STONE_PATH", "SQUARE", "PET_CENTER");
				openingBattles.recover(owner);
				openingMove(owner, "SQUARE", "STONE_PATH", "RIVERSIDE", "FOREST_EDGE");
			}
		}
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(forest, QuestOperation.DELIVER, "O006")).instance().status());
		openingMove(owner, "RIVERSIDE", "STONE_PATH", "SQUARE", "WEAPON_SHOP");
		var hides = acceptOpening(owner, "O007");
		long gold = queries.find(owner).gold();
		var hideDelivery = request(hides, QuestOperation.DELIVER, "O007");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, hideDelivery).instance().status());
		runtime.command(owner, hideDelivery);
		assertEquals(gold + 30, queries.find(owner).gold());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("LIFE_HIDE_0")));
		openingMove(owner, "SQUARE", "PET_CENTER");
		openingBattles.recover(owner);
		var capture = acceptOpening(owner, "O008");
		openingMove(owner, "SQUARE", "STONE_PATH", "RIVERSIDE");
		openingCapture(owner);
		openingMove(owner, "STONE_PATH", "SQUARE", "PET_CENTER");
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(capture, QuestOperation.DELIVER, "O008")).instance().status());
		openingBattles.recover(owner);
		openingMove(owner, "SQUARE", "STONE_PATH", "RIVERSIDE", "FOREST_EDGE", "CAVE_ENTRANCE");
		var boss = acceptOpening(owner, "O009");
		openingFight(owner, "OPENING_CAVE_BOSS");
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(boss, QuestOperation.DELIVER, "O009")).instance().status());
		var weapon = inventoryQueries.list(owner)
			.stream()
			.filter(item -> item.code().equals("CAVE_WEAPON"))
			.findFirst()
			.orElseThrow();
		inventoryCommands.equip(owner, Long.parseLong(weapon.id()));
		openingMove(owner, "FOREST_EDGE");
		var crate = acceptOpening(owner, "O010");
		openingMove(owner, "CAVE_ENTRANCE");
		sceneCommands.execute(owner, "O010_RECOVER",
				new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(UUID.randomUUID().toString(),
						queries.find(owner).revision(), null));
		openingMove(owner, "FOREST_EDGE");
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(crate, QuestOperation.DELIVER, "O010")).instance().status());
		var plains = acceptOpening(owner, "O011");
		openingMove(owner, "PLAIN_SIGN");
		openingFight(owner, "OPENING_PLAINS_PATROL");
		openingMove(owner, "STATION_OUTSKIRTS");
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(plains, QuestOperation.DELIVER, "O011")).instance().status());
		openingBattles.recover(owner);
		openingMove(owner, "PLAIN_SIGN");
		var order = acceptOpening(owner, "O012");
		openingMove(owner, "STATION_OUTSKIRTS");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(order, QuestOperation.DELIVER, "O012")));
		for (int i = 0; i < 4; i++) {
			openingMove(owner, "PLAIN_SIGN");
			openingFight(owner, "OPENING_PLAINS_PATROL");
			openingMove(owner, "STATION_OUTSKIRTS");
			openingBattles.recover(owner);
		}
		var orderDelivery = request(order, QuestOperation.DELIVER, "O012");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, orderDelivery).instance().status());
		var orderInventory = inventoryQueries.list(owner);
		runtime.command(owner, orderDelivery);
		assertEquals(orderInventory, inventoryQueries.list(owner));
		assertTrue(orderInventory.stream().noneMatch(item -> item.code().equals("LIFE_MEAT_0")));
		var body = orderInventory.stream()
			.filter(item -> item.code().equals("OPENING_ROAD_BODY"))
			.findFirst()
			.orElseThrow();
		inventoryCommands.equip(owner, Long.parseLong(body.id()));
		openingMove(owner, "ACADEMY");
		assertThrows(RuntimeException.class, () -> openingTraining.startAcademy(owner, "O013",
				List.of("T_CAT", "T_STONE", "T_SPROUT", "T_GALE"), UUID.randomUUID().toString()));
		var practice = acceptOpening(owner, "O013");
		var ownInventory = inventoryQueries.list(owner);
		var ownPets = creatureQueries.list(owner);
		String practiceKey = UUID.randomUUID().toString();
		var training = openingTraining.startAcademy(owner, "O013", List.of("T_CAT", "T_STONE", "T_SPROUT", "T_GALE"),
				practiceKey);
		var trained = BattleTestCommands.turn(openingBattles, owner, Long.parseLong(training.id()), training.round(),
				List.of(new io.github.lishangbu.avalon.battle.domain.BattleStateOrder("trainee", "DEFEND", "trainee"),
						new io.github.lishangbu.avalon.battle.domain.BattleStateOrder("T_CAT", "SWAP", "T_SPROUT"),
						new io.github.lishangbu.avalon.battle.domain.BattleStateOrder("T_STONE", "DEFEND", "T_STONE")),
				false);
		assertEquals("WON", trained.status());
		assertEquals(ownInventory, inventoryQueries.list(owner));
		assertEquals(ownPets, creatureQueries.list(owner));
		assertEquals(trained.id(),
				openingTraining
					.startAcademy(owner, "O013", List.of("T_CAT", "T_STONE", "T_SPROUT", "T_GALE"), practiceKey)
					.id());
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(practice, QuestOperation.DELIVER, "O013")).instance().status());
		var mine = acceptOpening(owner, "O014");
		openingMove(owner, "NOM_GATE_APPROACH", "CITY_OUTSKIRTS");
		openingFight(owner, "OPENING_MINE_ELITE");
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(mine, QuestOperation.DELIVER, "O014")).instance().status());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("OPENING_MINE_REPORT")));
		var arrival = acceptOpening(owner, "O015");
		for (int i = 0; i < 3; i++) {
			openingMove(owner, "GNOME_CITY", "CITY_PETS");
			openingBattles.recover(owner);
			openingMove(owner, "GNOME_CITY", "CITY_OUTSKIRTS");
			openingFight(owner, "OPENING_CITY_CLEAR");
			openingMove(owner, "GNOME_CITY");
			if (i < 2)
				assertThrows(RuntimeException.class,
						() -> runtime.command(owner, request(arrival, QuestOperation.DELIVER, "O015")));
			if (i < 2)
				openingMove(owner, "CITY_OUTSKIRTS");
		}
		var arrived = request(arrival, QuestOperation.DELIVER, "O015");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, arrived).instance().status());
		long finalXp = queries.find(owner).experience();
		runtime.command(owner, arrived);
		assertEquals(finalXp, queries.find(owner).experience());
		assertTrue(queries.find(owner).level() >= 8, "开篇任务经验应支持进入8级诺姆巡查");
		// 实走新开篇的角色可接收藏委托，其三种普通遭遇不再依赖旧章节。
		for (String species : List.of("CLAW_CAT", "SPROUT", "THUNDER_LIZARD")) {
			var encounter = battleCatalog.encounter("NOM_PLAINS_" + species);
			assertTrue(encounterAccess.allowed(owner, encounter));
			assertEquals("O003", encounter.captureRequires());
		}
		assertTrue(encounterAccess.allowed(owner, battleCatalog.encounter("NOM_TOWER_SPROUT")));
		openingMove(owner, "CITY_PETS");
		for (String quest : List.of("N037", "N038", "N039", "N040"))
			assertEquals(QuestStatus.ACTIVE, acceptOpening(owner, quest).status());
		openingBattles.recover(owner);
		openingMove(owner, "GNOME_CITY", "CITY_SHOP");
		var patrol = acceptOpening(owner, "N001");
		openingMove(owner, "GNOME_CITY", "CITY_OUTSKIRTS", "NOM_MINE_APPROACH");
		openingFight(owner, "NOM_MINE_PATROL");
		openingMove(owner, "CITY_OUTSKIRTS", "GNOME_CITY", "CITY_SHOP");
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(patrol, QuestOperation.DELIVER, "N001")).instance().status());
	}

	/** 纯引擎筛选成功样本后只固定该回合种子，实际捕获、扣球、事件与回执仍走正式事务。 */
	private void openingCapture(long owner) {
		var battle = openingBattles.start(owner, "OPENING_RIVERSIDE_PATROL", UUID.randomUUID().toString());
		String hero = battle.units().stream().filter(unit -> unit.hero()).findFirst().orElseThrow().id();
		String target = battle.units().stream().filter(unit -> unit.enemy()).findFirst().orElseThrow().id();
		var orders = battle.units()
			.stream()
			.filter(unit -> !unit.enemy() && (unit.hero() || unit.position() < 2))
			.map(unit -> new io.github.lishangbu.avalon.battle.domain.BattleStateOrder(unit.id(),
					unit.hero() ? "SEAL_I" : "DEFEND", unit.hero() ? target : unit.id()))
			.toList();
		var snapshot = new io.github.lishangbu.avalon.battle.domain.BattleResourceSnapshot(
				java.util.Map.of(hero, java.util.Map.of("SEAL_I", (long) battle.supplies().get("SEAL_I"))),
				java.util.Set.of(target));
		var state = transactions
			.call(() -> openingCodec.readValue(openingBattleRecords.get(Long.parseLong(battle.id())).getState(),
					io.github.lishangbu.avalon.battle.domain.BattleState.class));
		long seed = 0;
		while (true) {
			assertTrue(seed < 10000, "普通捕捉应存在可复现的成功样本");
			state.setSeed(seed);
			var result = openingEngine.advance(state, orders, false, snapshot, java.util.Map.of());
			if (result.state().getUnits().stream().anyMatch(unit -> unit.isEnemy() && unit.isCaptured()))
				break;
			seed++;
		}
		long selected = seed;
		transactions.run(() -> {
			var stored = openingBattleRecords.get(Long.parseLong(battle.id()));
			var current = openingCodec.readValue(stored.getState(),
					io.github.lishangbu.avalon.battle.domain.BattleState.class);
			current.setSeed(selected);
			stored.setState(openingCodec.writeValueAsString(current));
		});
		int before = creatureQueries.list(owner).size();
		var result = BattleTestCommands.turn(openingBattles, owner, Long.parseLong(battle.id()), battle.round(), orders,
				false);
		assertEquals("WON", result.status());
		assertEquals(before + 1, creatureQueries.list(owner).size());
	}

	/** 隔离验证事件窗口和正式回执；此用例不作为自然捕捉或首领难度验收。 */
	@Test
	void openingCaptureBossAndCrateSettleOnlyOnce() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeGate(owner, "O005");
		completeGate(owner, "O007");
		jdbc.update("UPDATE player_character SET location='PET_CENTER' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> facts.record(owner, "CAPTURE_WILD_LOCATION:RIVERSIDE"));
		var capture = acceptOpening(owner, "O008");
		transactions.run(() -> facts.record(owner, "CAPTURE_WILD_LOCATION:FOREST_EDGE"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(capture, QuestOperation.DELIVER, "O008")));
		transactions.run(() -> creatures.grant(owner, "OPENING_CAPTURE_FIXTURE", "SPROUT"));
		var pets = creatureQueries.list(owner);
		assertFalse(pets.isEmpty());
		transactions.run(() -> facts.record(owner, "CAPTURE_WILD_LOCATION:RIVERSIDE"));
		var captureDelivery = request(capture, QuestOperation.DELIVER, "O008");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, captureDelivery).instance().status());
		var captureInventory = inventoryQueries.list(owner);
		runtime.command(owner, captureDelivery);
		assertEquals(captureInventory, inventoryQueries.list(owner));
		// 任务经验会分配给出战伙伴；保留断言比较个体身份，不能把正常升级误判为扣宠。
		assertEquals(pets.stream().map(pet -> pet.id()).toList(),
				creatureQueries.list(owner).stream().map(pet -> pet.id()).toList());
		jdbc.update("UPDATE player_character SET location='CAVE_ENTRANCE' WHERE id=?", queries.requireId(owner));
		var boss = acceptOpening(owner, "O009");
		transactions.run(() -> facts.record(owner, "VICTORY:CAVE_BOSS"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(boss, QuestOperation.DELIVER, "O009")));
		transactions.run(() -> facts.record(owner, "VICTORY:OPENING_CAVE_BOSS"));
		assertTrue(openingBattleQueries.lobby(owner)
			.encounters()
			.stream()
			.anyMatch(encounter -> encounter.id().equals("OPENING_CAVE_BOSS")));
		var bossDelivery = request(boss, QuestOperation.DELIVER, "O009");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, bossDelivery).instance().status());
		var bossInventory = inventoryQueries.list(owner);
		assertTrue(bossInventory.stream().anyMatch(item -> item.code().equals("CAVE_WEAPON")));
		runtime.command(owner, bossDelivery);
		assertEquals(bossInventory, inventoryQueries.list(owner));
		var recovery = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		assertThrows(RuntimeException.class, () -> sceneCommands.execute(owner, "O010_RECOVER", recovery));
		openingMove(owner, "FOREST_EDGE");
		var crate = acceptOpening(owner, "O010");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(crate, QuestOperation.DELIVER, "O010")));
		openingMove(owner, "CAVE_ENTRANCE");
		var crateRequest = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		var recovered = sceneCommands.execute(owner, "O010_RECOVER", crateRequest);
		assertEquals(recovered, sceneCommands.execute(owner, "O010_RECOVER", crateRequest));
		openingMove(owner, "FOREST_EDGE");
		var delivery = request(crate, QuestOperation.DELIVER, "O010");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		var settled = inventoryQueries.list(owner);
		assertTrue(settled.stream().noneMatch(item -> item.code().equals("OPENING_SUPPLY_CRATE")));
		runtime.command(owner, delivery);
		assertEquals(settled, inventoryQueries.list(owner));
	}

	private void openingMove(long owner, String... locations) {
		for (String location : locations)
			characters.move(owner, location, queries.find(owner).revision());
	}

	private void openingRiversideTrip(long owner) {
		openingMove(owner, "SQUARE", "STONE_PATH", "RIVERSIDE");
		openingFight(owner, "OPENING_RIVERSIDE_PATROL");
		openingMove(owner, "STONE_PATH", "SQUARE", "PET_CENTER");
		openingBattles.recover(owner);
	}

	/** 使用公开战局选招、药剂与替补，不修改种子、敌人血量、人物等级或资源。 */
	private void openingFight(long owner, String encounter) {
		var battle = openingBattles.start(owner, encounter, UUID.randomUUID().toString());
		int rounds = 0;
		while (battle.status().equals("ACTIVE")) {
			assertTrue(rounds++ < 30, "初级遭遇应在30回合内完成");
			var replacements = new java.util.LinkedHashMap<String, Integer>();
			var reserves = battle.units()
				.stream()
				.filter(unit -> !unit.enemy() && !unit.hero() && unit.hp() > 0 && unit.position() >= 2
						&& unit.position() <= 3)
				.iterator();
			for (int position = 0; position < 2 && reserves.hasNext(); position++) {
				int slot = position;
				if (battle.units()
					.stream()
					.noneMatch(unit -> !unit.enemy() && !unit.hero() && unit.position() == slot && unit.hp() > 0))
					replacements.put(reserves.next().id(), position);
			}
			var party = battle.units()
				.stream()
				.filter(unit -> !unit.enemy() && unit.hp() > 0
						&& (unit.hero() || unit.position() < 2 || replacements.containsKey(unit.id())))
				.toList();
			var injured = party.stream()
				.min(java.util.Comparator.comparingDouble(unit -> (double) unit.hp() / unit.stats().health()))
				.orElseThrow();
			boolean warning = battle.telegraph() != null && battle.telegraph().contains("即将");
			String target = battle.units()
				.stream()
				.filter(unit -> unit.enemy() && unit.hp() > 0)
				.findFirst()
				.orElseThrow()
				.id();
			var orders = party.stream().map(unit -> {
				String action = warning ? "DEFEND" : "ATTACK";
				String aim = warning ? unit.id() : target;
				if (unit.hero() && injured.hp() < injured.stats().health() * 0.45
						&& unit.actions()
							.stream()
							.anyMatch(value -> value.id().equals("HEALTH_POTION") && value.available())) {
					action = "HEALTH_POTION";
					aim = injured.id();
				}
				else if (!warning && !unit.hero()
						&& unit.actions().stream().anyMatch(value -> value.id().equals("水弹") && value.available()))
					action = "水弹";
				return new io.github.lishangbu.avalon.battle.domain.BattleStateOrder(unit.id(), action, aim);
			}).toList();
			battle = BattleTestCommands.turn(openingBattles, owner, Long.parseLong(battle.id()), battle.round(), orders,
					false, replacements);
		}
		assertEquals("WON", battle.status());
	}

	/** 前哨胜利与钥件同时交付才永久开门，主线封印室进度不作前置。 */
	@Test
	void relicOutpostKeyOpensIndependentOptionalDungeonEntrance() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O014", "O015"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N056");
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_RELIC_OUTPOST"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N056")));
		transactions.run(() -> inventoryCommands.grant(owner, "OUTPOST_KEY", "NOM_RELIC_KEY", 1, false));
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N056");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("NOM_RELIC_KEY")));
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		for (String location : List.of("GNOME_OLD_TRAIL", "GNOME_MINE_GATE", "NOM_RELIC_VAULT_ENTRANCE",
				"GNOME_MINE_GATE", "NOM_RELIC_VAULT_ENTRANCE"))
			characters.move(owner, location, queries.find(owner).revision());
		assertEquals("NOM_RELIC_VAULT_ENTRANCE", queries.find(owner).location());
		assertFalse(questQueries.done(owner, "N020"));
	}

	/** 生产点必须有接取后的指定捕捉，交付后真实开放可往返药圃。 */
	@Test
	void productionCaptureOpensRestoredHerbGardenAndRetainsPets() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O014", "O015", "N054"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> characters.move(owner, "NOM_RESTORED_HERB_GARDEN", queries.find(owner).revision()));
		jdbc.update("UPDATE player_character SET location='CITY_PETS' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N055");
		transactions.run(() -> facts.record(owner, "CAPTURE:SPROUT"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N055")));
		transactions.run(() -> facts.record(owner, "CAPTURE_ENCOUNTER:NOM_PRODUCTION_SPROUT"));
		var pets = creatureQueries.list(owner);
		var delivery = request(state, QuestOperation.DELIVER, "N055");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(pets, creatureQueries.list(owner));
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		characters.move(owner, "NOM_RESTORED_HERB_GARDEN", queries.find(owner).revision());
		characters.move(owner, "CITY_OUTSKIRTS", queries.find(owner).revision());
		characters.move(owner, "NOM_RESTORED_HERB_GARDEN", queries.find(owner).revision());
		assertEquals("NOM_RESTORED_HERB_GARDEN", queries.find(owner).location());
	}

	/** 人物装备教学只认可接取后的穿戴武器胜利，不消费装备。 */
	@Test
	void equipmentTrialRequiresEquippedVictoryAfterAcceptance() {
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O015"))
			completeGate(owner, gate);
		transactions.run(() -> facts.record(owner, "VICTORY_EQUIPPED_SLOT:WEAPON"));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N035");
		transactions.run(() -> facts.record(owner, "KILL:CLAW_CAT"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N035")));
		transactions.run(() -> facts.record(owner, "VICTORY_EQUIPPED_SLOT:WEAPON"));
		var before = inventoryQueries.list(owner);
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N035");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
		assertEquals(before, inventoryQueries.list(owner));
	}

	/** 工程汇合后的试运行必须有现场操作和指定清理，普通击杀不算。 */
	@Test
	void workshopTrialRequiresOperationAndSpecificPatrol() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O014", "N053"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N054");
		transactions.run(() -> facts.record(owner, "KILL:CLAW_CAT"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N054")));
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		var operation = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		var result = sceneCommands.execute(owner, "N054_OPERATE", operation);
		assertEquals(result, sceneCommands.execute(owner, "N054_OPERATE", operation));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N054")));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_WORKSITE_PATROL"));
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N054");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 商道五场胜利与包裹交付同时满足，恢复供货只发一次。 */
	@Test
	void tradeRoadRequiresFivePatrolsAndParcelThenRestoresSupplyOnce() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O014", "N029"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N030");
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		for (int i = 0; i < 4; i++)
			transactions.run(() -> facts.record(owner, "VICTORY:NOM_TRADE_ROAD_PATROL"));
		characters.move(owner, "GNOME_CITY", queries.find(owner).revision());
		characters.move(owner, "CITY_OUTSKIRTS", queries.find(owner).revision());
		sceneCommands.execute(owner, "N030_RECOVER",
				new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(UUID.randomUUID().toString(),
						queries.find(owner).revision(), null));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N030")));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_TRADE_ROAD_PATROL"));
		int potions = inventoryQueries.list(owner)
			.stream()
			.filter(item -> item.code().equals("HEALTH_POTION"))
			.mapToInt(item -> item.quantity())
			.sum();
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N030");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("NOM_SUPPLY_PARCEL")));
		assertEquals(potions + 2,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("HEALTH_POTION"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	/** 可选遗物委托只接受接取后的副本核心胜利，不能用普通击杀替代。 */
	@Test
	void relicGuardianRequiresNewDungeonCoreVictory() {
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O015", "N058", "N056"))
			completeGate(owner, gate);
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_RELIC_KEEPER"));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N059");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N059")));
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_SEAL_OUTER"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N059")));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_RELIC_KEEPER"));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N059");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(xp + 1200, queries.find(owner).experience());
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 坐标验证必须有新胜利和实际部件，交付后可按任意顺序进入两城。 */
	@Test
	void coordinateDeliveryOpensBothCityRoutesWithoutLockingAnOrder() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O014", "N024"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N025");
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> characters.move(owner, "GNOME_PORT_ROAD", queries.find(owner).revision()));
		assertThrows(RuntimeException.class,
				() -> characters.move(owner, "SYLPH_PASS", queries.find(owner).revision()));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_COORDINATE_TEST"));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N025")));
		transactions.run(() -> inventoryCommands.grant(owner, "COORDINATE_TEST_LOOT", "NOM_COORDINATE_PART", 1, false));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N025");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("NOM_COORDINATE_PART")));
		runtime.command(owner, delivery);
		assertEquals(xp + 2000, queries.find(owner).experience());
		assertEquals(gold + 50, queries.find(owner).gold());
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		for (String location : List.of("SYLPH_PASS", "SYLPH_CITY", "SYLPH_PASS", "CITY_OUTSKIRTS", "GNOME_PORT_ROAD",
				"UNDINE_CITY", "GNOME_PORT_ROAD", "CITY_OUTSKIRTS", "SYLPH_PASS", "SYLPH_CITY"))
			characters.move(owner, location, queries.find(owner).revision());
		assertEquals("SYLPH_CITY", queries.find(owner).location());
	}

	/** 返回通路清理后两件现场资产必须齐备，交付消耗且奖励不重复。 */
	@Test
	void postDungeonRecoveryRequiresClearAndBothRealSceneAssets() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "N019", "N023"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='NOM_SEAL_FORECOURT' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N024_CLEAR");
		var recordRequest = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		assertThrows(RuntimeException.class, () -> sceneCommands.execute(owner, "N024_RECORD", recordRequest));
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		assertFalse(questQueries.done(owner, "N024_CLEAR"));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_RETURN_GUARD"));
		assertTrue(questQueries.done(owner, "N024_CLEAR"));
		var recovered = sceneCommands.execute(owner, "N024_RECORD", recordRequest);
		assertEquals(recovered, sceneCommands.execute(owner, "N024_RECORD", recordRequest));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N024")));
		jdbc.update("UPDATE player_character SET location='NOM_SEAL_FORECOURT' WHERE id=?", queries.requireId(owner));
		sceneCommands.execute(owner, "N024_SHARD",
				new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(UUID.randomUUID().toString(),
						queries.find(owner).revision(), null));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N024");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().startsWith("NOM_CORE_")));
		runtime.command(owner, delivery);
		assertEquals(xp + 1800, queries.find(owner).experience());
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 入本前同时登记三项委托，指定阶段按序结算，不用退出接下一项。 */
	@Test
	void dungeonAssignmentsAcceptTogetherAndCompleteOnlyTheirOrderedStages() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeGate(owner, "N019");
		completeGate(owner, "N020");
		jdbc.update("UPDATE player_character SET location='NOM_SEAL_FORECOURT' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_SEAL_OUTER"));
		accept(owner, "N021");
		accept(owner, "N022_BRIEFING");
		accept(owner, "N023_BRIEFING");
		assertFalse(questQueries.done(owner, "N021"));
		assertFalse(questQueries.done(owner, "N022"));
		assertFalse(questQueries.done(owner, "N023"));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		assertFalse(questQueries.done(owner, "N021"));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_SEAL_OUTER"));
		assertTrue(questQueries.done(owner, "N021"));
		assertFalse(questQueries.done(owner, "N022"));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_SEAL_CREATION"));
		assertTrue(questQueries.done(owner, "N022"));
		assertFalse(questQueries.done(owner, "N023"));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_SEAL_CORE"));
		assertTrue(questQueries.done(owner, "N023"));
		assertEquals(xp + 3800, queries.find(owner).experience());
		assertEquals(gold + 90, queries.find(owner).gold());
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_SEAL_CORE"));
		assertEquals(xp + 3800, queries.find(owner).experience());
		assertEquals(gold + 90, queries.find(owner).gold());
	}

	/** 前庭需接取后清理指定哨卫，启动必须在现场且只奖励一次。 */
	@Test
	void forecourtRequiresNewVictoryBeforeLocalActivation() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "N019"))
			completeGate(owner, gate);
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_FORECOURT_SENTINEL"));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N020_CLEAR");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N020")));
		jdbc.update("UPDATE player_character SET location='NOM_RUIN_INNER_COURT' WHERE id=?", queries.requireId(owner));
		characters.move(owner, "NOM_SEAL_FORECOURT", queries.find(owner).revision());
		assertFalse(questQueries.done(owner, "N020_CLEAR"));
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		assertFalse(questQueries.done(owner, "N020_CLEAR"));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_FORECOURT_SENTINEL"));
		assertTrue(questQueries.done(owner, "N020_CLEAR"));
		assertFalse(questQueries.done(owner, "N020"));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N020");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(xp + 188000, queries.find(owner).experience());
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 入口门禁只认接取后的指定守卫胜利，真正进入地下后才结算，往返不重复发奖。 */
	@Test
	void undergroundEntryRequiresNewGuardVictoryAndActualArrival() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "N013", "N015"))
			completeGate(owner, gate);
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_UNDERGROUND_GUARD"));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		accept(owner, "N016_CLEAR");
		jdbc.update("UPDATE player_character SET location='NOM_OLD_DEVICE' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> characters.move(owner, "NOM_UNDERGROUND_ENTRY", queries.find(owner).revision()));
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		assertFalse(questQueries.done(owner, "N016_CLEAR"));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_UNDERGROUND_GUARD"));
		assertTrue(questQueries.done(owner, "N016_CLEAR"));
		assertFalse(questQueries.done(owner, "N016"));
		long gold = queries.find(owner).gold();
		long xp = queries.find(owner).experience();
		characters.move(owner, "NOM_UNDERGROUND_ENTRY", queries.find(owner).revision());
		assertTrue(questQueries.done(owner, "N016"));
		characters.move(owner, "NOM_OLD_DEVICE", queries.find(owner).revision());
		characters.move(owner, "NOM_UNDERGROUND_ENTRY", queries.find(owner).revision());
		assertEquals(gold + 30, queries.find(owner).gold());
		assertEquals(xp + 95000, queries.find(owner).experience());
	}

	/** 部件安装、停用和验证顺序不可跳过，部件只扣一次，验证遭遇在停用后开放。 */
	@Test
	void deviceShutdownRequiresInstalledPartBeforeVerificationAndRewardsOnce() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "N013", "N014"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N015_INSTALL");
		var verification = battleCatalog.encounter("NOM_DEVICE_VERIFICATION");
		assertFalse(encounterAccess.allowed(owner, verification));
		jdbc.update("UPDATE player_character SET location='NOM_OLD_DEVICE' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N015_SHUTDOWN")));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N015_INSTALL")));
		transactions.run(() -> inventoryCommands.grant(owner, "SHUTDOWN_PART", "NOM_REPLACEMENT_PART", 1, false));
		var installation = request(state, QuestOperation.DELIVER, "N015_INSTALL");
		runtime.command(owner, installation);
		assertTrue(questQueries.done(owner, "N015_INSTALL"));
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("NOM_REPLACEMENT_PART")));
		assertFalse(encounterAccess.allowed(owner, verification));
		runtime.command(owner, request(state, QuestOperation.DELIVER, "N015_SHUTDOWN"));
		assertTrue(encounterAccess.allowed(owner, verification));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_DEVICE_ELITE"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N015")));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_DEVICE_VERIFICATION"));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N015");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(xp + 76000, queries.find(owner).experience());
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 两场战斗之外还要真实到场；先到场、后补战斗仍需再次进入完成。 */
	@Test
	void tunnelQuestRequiresTwoNewVictoriesAndActualArrival() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O014", "O015", "N004"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> {
			facts.record(owner, "VICTORY:NOM_TUNNEL_PATROL");
			facts.record(owner, "VICTORY:NOM_TUNNEL_PATROL");
		});
		accept(owner, "N005");
		for (String place : List.of("GNOME_CITY", "CITY_OUTSKIRTS", "NOM_MINE_APPROACH", "NOM_MINE_PASSAGE"))
			characters.move(owner, place, queries.find(owner).revision());
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_TUNNEL_PATROL"));
		characters.move(owner, "NOM_ABANDONED_POST", queries.find(owner).revision());
		assertFalse(questQueries.done(owner, "N005"));
		characters.move(owner, "NOM_MINE_PASSAGE", queries.find(owner).revision());
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_TUNNEL_PATROL"));
		assertFalse(questQueries.done(owner, "N005"));
		long gold = queries.find(owner).gold();
		long xp = queries.find(owner).experience();
		characters.move(owner, "NOM_ABANDONED_POST", queries.find(owner).revision());
		assertTrue(questQueries.done(owner, "N005"));
		assertEquals(gold + 30, queries.find(owner).gold());
		assertEquals(xp + 5000, queries.find(owner).experience());
		characters.move(owner, "NOM_MINE_PASSAGE", queries.find(owner).revision());
		characters.move(owner, "NOM_ABANDONED_POST", queries.find(owner).revision());
		assertEquals(gold + 30, queries.find(owner).gold());
		assertEquals(xp + 5000, queries.find(owner).experience());
	}

	/** 哨所遗物必须在接取及胜利后现场回收，收集回执与交付均不可重复发放。 */
	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.CsvSource({ "N006,NOM_ABANDONED_POST,NOM_POST_GUARD,NOM_POST_RELIC,600,N005,1",
			"N010,NOM_INNER_SEAL,NOM_SEAL_GUARD,NOM_SEAL_FRAGMENT,17000,N009,1",
			"N011,NOM_OLD_YARD,NOM_YARD_PATROL,NOM_SHIPPING_MANIFEST,650,N010,1",
			"N012,NOM_OLD_YARD,NOM_YARD_PATROL,NOM_UNSTABLE_COMPONENT,30000,N011,2" })
	void recoveredRelicRequiresGuardVictoryAndIsConsumedOnce(String quest, String location, String guard, String item,
			int experience, String gate, int quantity) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String prerequisite : List.of("O013", "O014", "O015", "N004", gate))
			completeGate(owner, prerequisite);
		if (quest.equals("N012"))
			completeGate(owner, "N010");
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, quest);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", location, queries.requireId(owner));
		var recovery = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		assertThrows(RuntimeException.class, () -> sceneCommands.execute(owner, quest + "_RECOVER", recovery));
		transactions.run(() -> facts.record(owner, "VICTORY:" + guard));
		var result = sceneCommands.execute(owner, quest + "_RECOVER", recovery);
		assertEquals(quantity, result.quantity());
		assertEquals(result, sceneCommands.execute(owner, quest + "_RECOVER", recovery));
		assertEquals(0,
				sceneCommands
					.execute(owner, quest + "_RECOVER",
							new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
									UUID.randomUUID().toString(), queries.find(owner).revision(), null))
					.quantity());
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var delivery = request(state, QuestOperation.DELIVER, quest);
		long xp = queries.find(owner).experience();
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(value -> value.code().equals(item)));
		assertEquals(xp + experience, queries.find(owner).experience());
		runtime.command(owner, delivery);
		assertEquals(xp + experience, queries.find(owner).experience());
	}

	/** 两种物资齐全且现场确认才开门，拒绝时不扣料，成功后可正常进出且不重发奖励。 */
	@Test
	void stoneDoorRequiresKeyAndOreAtTheGateBeforeOpeningInnerChamber() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "N007", "N008"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N009");
		transactions.run(() -> inventoryCommands.grant(owner, "DOOR_ORE", "LIFE_ORE_0", 4, false));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N009")));
		jdbc.update("UPDATE player_character SET location='NOM_STONE_GATE' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> characters.move(owner, "NOM_INNER_SEAL", queries.find(owner).revision()));
		var before = inventoryQueries.list(owner);
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N009")));
		assertEquals(before, inventoryQueries.list(owner));
		transactions.run(() -> inventoryCommands.grant(owner, "DOOR_KEY", "NOM_STONE_KEY", 1, false));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N009")));
		jdbc.update("UPDATE player_character SET location='NOM_STONE_GATE' WHERE id=?", queries.requireId(owner));
		long gold = queries.find(owner).gold();
		long xp = queries.find(owner).experience();
		var delivery = request(state, QuestOperation.DELIVER, "N009");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(inventoryQueries.list(owner)
			.stream()
			.noneMatch(item -> List.of("NOM_STONE_KEY", "LIFE_ORE_0").contains(item.code())));
		characters.move(owner, "NOM_INNER_SEAL", queries.find(owner).revision());
		assertEquals("NOM_INNER_SEAL", queries.find(owner).location());
		characters.move(owner, "NOM_STONE_GATE", queries.find(owner).revision());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
		assertEquals(xp + 500, queries.find(owner).experience());
	}

	/** 既有残片可交付，矿石不能替代；仅扣三份并解锁正式首领入口。 */
	@Test
	void guardFragmentsConsumeThreeAndUnlockGatekeeper() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeOpening(owner);
		completeGate(owner, "N006");
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> {
			inventoryCommands.grant(owner, "GUARD_FRAGMENTS", "NOM_GUARD_FRAGMENT", 2, false);
			inventoryCommands.grant(owner, "GUARD_WRONG_ORE", "LIFE_ORE_0", 3, false);
		});
		var state = accept(owner, "N007");
		assertFalse(encounterAccess.allowed(owner, battleCatalog.encounter("NOM_MINE_GATEKEEPER")));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N007")));
		transactions.run(() -> inventoryCommands.grant(owner, "GUARD_FRAGMENTS_EXTRA", "NOM_GUARD_FRAGMENT", 2, false));
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N007");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(encounterAccess.allowed(owner, battleCatalog.encounter("NOM_MINE_GATEKEEPER")));
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("NOM_GUARD_FRAGMENT"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(3,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_ORE_0"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(gold + 30, queries.find(owner).gold());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 开篇战斗目标排除历史与异地事件，并按实际数量发放一次明确经验奖励。 */
	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.CsvSource({ "N001,VICTORY:NOM_MINE_PATROL,1,3300,O015",
			"N002,KILL_LOCATION:NOM_MINE_APPROACH,6,5000,N001", "N013,VICTORY:NOM_TRANSPORT_CLEAR,3,102000,N012" })
	void openingMineCombatRequiresNewCorrectEventsAndAwardsExperienceOnce(String quest, String event, int amount,
			int experience, String gate) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeOpening(owner);
		completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> {
			for (int i = 0; i < amount; i++)
				facts.record(owner, event);
		});
		var state = accept(owner, quest);
		transactions.run(() -> {
			for (int i = 0; i < amount; i++) {
				facts.record(owner, "KILL:STONE_GOLEM");
				facts.record(owner, "KILL_LOCATION:CITY_OUTSKIRTS");
			}
			for (int i = 0; i < amount - 1; i++)
				facts.record(owner, event);
		});
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, quest)));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		transactions.run(() -> facts.record(owner, event));
		var delivery = request(state, QuestOperation.DELIVER, quest);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(xp + experience, queries.find(owner).experience());
		assertEquals(gold + 30, queries.find(owner).gold());
		runtime.command(owner, delivery);
		assertEquals(xp + experience, queries.find(owner).experience());
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 支撑任务与兽群清理并行，既有矿石有效；不足不扣、补齐扣六份且不重复发奖。 */
	@Test
	void mineSupportsCanBeDeliveredBeforeBeastClearance() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeOpening(owner);
		completeGate(owner, "N001");
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> inventoryCommands.grant(owner, "SUPPORT_ORE", "LIFE_ORE_0", 5, false));
		var state = accept(owner, "N003");
		assertFalse(questQueries.done(owner, "N002"));
		var before = inventoryQueries.list(owner);
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N003")));
		assertEquals(before, inventoryQueries.list(owner));
		transactions.run(() -> inventoryCommands.grant(owner, "SUPPORT_ORE_LAST", "LIFE_ORE_0", 1, false));
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N003");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("LIFE_ORE_0")));
		assertEquals(gold + 30, queries.find(owner).gold());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 正式主线OR要先满足两项前置，完成前切换、任一路完成后汇合且只收所选材料。 */
	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "FIGHT", "REPAIR" })
	void mineRouteSupportsBothMethodsAndJoinsIntoNextQuest(String option) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeOpening(owner);
		completeGate(owner, "N003");
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class, () -> accept(owner, "N004_FIGHT"));
		completeGate(owner, "N002");
		transactions.run(() -> inventoryCommands.grant(owner, "ROUTE_WOOD", "LIFE_WOOD_0", 4, false));
		var state = accept(owner, "N004_FIGHT");
		for (String selected : List.of(option.equals("FIGHT") ? "REPAIR" : "FIGHT", option))
			runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), state.round(),
					QuestOperation.CHOOSE, null, "NOM_MINE_ROUTE", selected));
		long gold = queries.find(owner).gold();
		if (option.equals("FIGHT")) {
			transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N004_FIGHT")));
			transactions.run(() -> facts.record(owner, "VICTORY:NOM_OLD_ROAD_GUARD"));
		}
		var delivery = request(state, QuestOperation.DELIVER, "N004_" + option);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(questQueries.done(owner, "N004"));
		assertEquals(option.equals("FIGHT") ? 4 : 0,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_WOOD_0"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(gold + 30, queries.find(owner).gold());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner,
						new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), state.round(),
								QuestOperation.CHOOSE, null, "NOM_MINE_ROUTE",
								option.equals("FIGHT") ? "REPAIR" : "FIGHT")));
		assertNotNull(accept(owner, "N005"));
	}

	/** 矿场入口要求N007，支持沿正式道路往返。 */
	@Test
	void stoneGateRouteOpensAfterFragmentsWithoutLegacyMineGate() {
		long owner = create();
		completeGate(owner, "O013");
		completeGate(owner, "O015");
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> characters.move(owner, "NOM_STONE_GATE", queries.find(owner).revision()));
		completeGate(owner, "N007");
		characters.move(owner, "NOM_STONE_GATE", queries.find(owner).revision());
		assertEquals("NOM_STONE_GATE", queries.find(owner).location());
		characters.move(owner, "CITY_OUTSKIRTS", queries.find(owner).revision());
		assertEquals("CITY_OUTSKIRTS", queries.find(owner).location());
		openingMove(owner, "GNOME_OLD_TRAIL", "GNOME_MINE_GATE", "NOM_DEEP_CAVERN", "NOM_STONE_GATE", "CITY_OUTSKIRTS");
		assertTrue(encounterAccess.allowed(owner, battleCatalog.encounter("NOM_CAVERN_GOLEM")));
	}

	/** 两项正式首领委托要求新胜利，人物装备与金币只发放一次。 */
	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.CsvSource({ "N049,NOM_WATCHTOWER_BOSS,NOM_TOWER_RELIC,O015",
			"N008,NOM_MINE_GATEKEEPER,NOM_MINER_COAT,N007", "N014,NOM_DEVICE_ELITE,NOM_FIELD_HELM,N013" })
	void bossQuestRequiresNewVictoryAndAwardsHumanEquipmentOnce(String quest, String encounter, String item,
			String gate) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeOpening(owner);
		completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> facts.record(owner, "VICTORY:" + encounter));
		var state = accept(owner, quest);
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, quest)));
		transactions.run(() -> facts.record(owner, "VICTORY:" + encounter));
		var delivery = request(state, QuestOperation.DELIVER, quest);
		long gold = queries.find(owner).gold();
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(1, inventoryQueries.list(owner).stream().filter(value -> value.code().equals(item)).count());
		assertEquals(gold + 30, queries.find(owner).gold());
		var equipment = inventoryQueries.list(owner);
		runtime.command(owner, delivery);
		assertEquals(equipment, inventoryQueries.list(owner));
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 正式三物种收藏要求同时持有；重复同种不能抵另一种，结算和重放均不收走伙伴。 */
	@Test
	void plainsCollectionRequiresAllThreeOwnedSpeciesWithoutConsumingThem() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeGate(owner, "O015");
		completeOpening(owner);
		jdbc.update("UPDATE player_character SET location='CITY_PETS' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> {
			creatures.grant(owner, "COLLECTION_CAT", "CLAW_CAT");
			creatures.grant(owner, "COLLECTION_CAT_AGAIN", "CLAW_CAT");
			creatures.grant(owner, "COLLECTION_SPROUT", "SPROUT");
		});
		var state = accept(owner, "N040");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N040")));
		transactions.run(() -> creatures.grant(owner, "COLLECTION_LIZARD", "THUNDER_LIZARD"));
		var before = creatureQueries.list(owner);
		var delivery = request(state, QuestOperation.DELIVER, "N040");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(before, creatureQueries.list(owner));
		runtime.command(owner, delivery);
		assertEquals(before, creatureQueries.list(owner));
	}

	/** 正式两步任务与现场门禁：清理后开放指定普通遭遇，其他来源捕捉不算。 */
	@Test
	void deepCavernCaptureOpensOnlyAfterEliteAndRequiresItsOwnEncounter() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		// 为隔离两步捕捉规则补建新主线前置回执，目标和解锁均使用正式内容。
		completeGate(owner, "N007");
		completeOpening(owner);
		jdbc.update("UPDATE player_character SET location='NOM_DEEP_CAVERN' WHERE id=?", queries.requireId(owner));
		var moth = battleCatalog.encounter("NOM_CAVERN_MOTH");
		assertFalse(encounterAccess.allowed(owner, moth));
		var state = accept(owner, "N044_CLEAR");
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		assertFalse(questQueries.done(owner, "N044_CLEAR"));
		assertFalse(encounterAccess.allowed(owner, moth));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_CAVERN_ELITE"));
		assertTrue(questQueries.done(owner, "N044_CLEAR"));
		assertTrue(encounterAccess.allowed(owner, moth));
		jdbc.update("UPDATE player_character SET location='CITY_PETS' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> facts.record(owner, "CAPTURE:MOONLIGHT_MOTH"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N044")));
		transactions.run(() -> facts.record(owner, "CAPTURE_ENCOUNTER:NOM_CAVERN_MOTH"));
		var delivery = request(state, QuestOperation.DELIVER, "N044");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(3,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("SEAL_II"))
					.mapToInt(item -> item.quantity())
					.sum());
		var settled = inventoryQueries.list(owner);
		runtime.command(owner, delivery);
		assertEquals(settled, inventoryQueries.list(owner));
	}

	/** 使用正式任务资产和正式现场来源，验证先接取、到场回收、不可刷取及最终扣除。 */
	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "N048", "N051" })
	void recoveredTaskAssetsRequireAcceptanceAndAreDeliveredOnce(String id) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		var content = catalog.step(id);
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, null);
		var base = catalog.step(id);
		var step = new QuestContentStep(id, id, content.title(), base.location(), content.label(), content.dialogue(),
				List.of(), true, content.effects(), content.objectives(), List.of(), QuestDelivery.MANUAL, null, null,
				null, true, List.of(), new QuestReception(base.location(), null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(value -> value.id().equals(id));
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		long owner = create();
		String action = id + "_RECOVER";
		// 隔离回收事务，区域使用完整新开篇前置；不跳过接取与现场资产校验。
		completeOpening(owner);
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> sceneCommands.execute(owner, action,
						new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
								UUID.randomUUID().toString(), queries.find(owner).revision(), null)));
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", base.location(), queries.requireId(owner));
		var state = accept(owner, id);
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		var recovery = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		var result = sceneCommands.execute(owner, action, recovery);
		assertEquals(id.equals("N048") ? 1 : 4, result.quantity());
		var recovered = inventoryQueries.list(owner);
		assertEquals(result, sceneCommands.execute(owner, action, recovery));
		assertEquals(0,
				sceneCommands
					.execute(owner, action,
							new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
									UUID.randomUUID().toString(), queries.find(owner).revision(), null))
					.quantity());
		assertEquals(recovered, inventoryQueries.list(owner));
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", base.location(), queries.requireId(owner));
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, id);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals(result.itemCode())));
		assertEquals(gold + 30, queries.find(owner).gold());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 指定精英只认接取后的指定遭遇胜利，不认历史胜利或同物种普通击杀。 */
	@Test
	void tradeRoadEliteRequiresNewDesignatedVictory() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		var content = catalog.step("N029");
		String id = "TEST_TRADE_ROAD_ELITE";
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, null);
		var base = catalog.step(id);
		var step = new QuestContentStep(id, id, content.title(), base.location(), content.label(), content.dialogue(),
				List.of(), true, content.effects(), content.objectives(), List.of(), QuestDelivery.MANUAL, null, null,
				null, true, List.of(), new QuestReception(base.location(), null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(value -> value.id().equals(id));
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		long owner = create();
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_TRADE_ROAD_ELITE"));
		var state = accept(owner, id);
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		transactions.run(() -> {
			facts.record(owner, "KILL:CLAW_CAT");
			facts.record(owner, "VICTORY:PLAINS");
		});
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		long gold = queries.find(owner).gold();
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_TRADE_ROAD_ELITE"));
		var delivery = request(state, QuestOperation.DELIVER, id);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(gold + 30, queries.find(owner).gold());
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("JOURNEY_HANDS"))
					.mapToInt(item -> item.quantity())
					.sum());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("JOURNEY_HANDS"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	/** 两个指定精英各自需要新胜利和独立印记，只有一种印记不能领取装备或被提前扣除。 */
	@Test
	void eliteMarksRequireBothVictoriesAndConsumeBothMarksOnce() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		var content = catalog.step("N047");
		String id = "TEST_ELITE_MARKS";
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, null);
		var base = catalog.step(id);
		var step = new QuestContentStep(id, id, content.title(), base.location(), content.label(), content.dialogue(),
				List.of(), true, content.effects(), content.objectives(), List.of(), QuestDelivery.MANUAL, null, null,
				null, true, List.of(), new QuestReception(base.location(), null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(value -> value.id().equals(id));
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		long owner = create();
		var state = accept(owner, id);
		transactions.run(() -> {
			facts.record(owner, "VICTORY:NOM_QUARRY_ELITE");
			facts.record(owner, "VICTORY:NOM_QUARRY_ELITE");
			inventoryCommands.grant(owner, "TWO_QUARRY_MARKS", "NOM_QUARRY_MARK", 2, false);
		});
		var before = inventoryQueries.list(owner);
		long gold = queries.find(owner).gold();
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		assertEquals(before, inventoryQueries.list(owner));
		assertEquals(gold, queries.find(owner).gold());
		// 只拿到另一枚印记仍不算亲自完成接取后的另一处悬赏。
		transactions.run(() -> inventoryCommands.grant(owner, "CAMP_MARK", "NOM_CAMP_MARK", 1, false));
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_CAMP_ELITE"));
		var delivery = request(state, QuestOperation.DELIVER, id);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("NOM_QUARRY_MARK"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("NOM_CAMP_MARK")));
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("PLAIN_LEGS"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(gold + 30, queries.find(owner).gold());
		var settled = inventoryQueries.list(owner);
		runtime.command(owner, delivery);
		assertEquals(settled, inventoryQueries.list(owner));
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 正式残件委托须交足六份；不足不扣料，成功扣料和报酬只结算一次。 */
	@Test
	void relicFragmentsRequireFullDeliveryAndSettleOnce() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		var content = catalog.step("N058");
		String id = "TEST_RELIC_DELIVERY";
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, null);
		var base = catalog.step(id);
		var step = new QuestContentStep(id, id, content.title(), base.location(), content.label(), content.dialogue(),
				List.of(), true, content.effects(), content.objectives(), List.of(), QuestDelivery.MANUAL, null, null,
				null, true, List.of(), new QuestReception(base.location(), null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(value -> value.id().equals(id));
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		long owner = create();
		var state = accept(owner, id);
		transactions.run(() -> inventoryCommands.grant(owner, "RELIC_FIVE", "NOM_RELIC_FRAGMENT", 5, false));
		var before = inventoryQueries.list(owner);
		long gold = queries.find(owner).gold();
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		assertEquals(before, inventoryQueries.list(owner));
		assertEquals(gold, queries.find(owner).gold());
		transactions.run(() -> inventoryCommands.grant(owner, "RELIC_LAST", "NOM_RELIC_FRAGMENT", 1, false));
		var delivery = request(state, QuestOperation.DELIVER, id);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("NOM_RELIC_FRAGMENT")));
		assertEquals(gold + 30, queries.find(owner).gold());
		var settled = inventoryQueries.list(owner);
		runtime.command(owner, delivery);
		assertEquals(settled, inventoryQueries.list(owner));
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 两种材料必须分别足量；失败不消费，成功与报酬共同结算且不可重放扣料。 */
	@Test
	void twoMaterialDeliveryRequiresBothKindsAndConsumesOnlyOnce() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		// 使用正式N052的目标与效果，测试场景仅移到出生点并简化前置以隔离事务验证。
		var content = catalog.step("N052");
		String id = "TEST_TWO_MATERIALS";
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, null);
		var base = catalog.step(id);
		var step = new QuestContentStep(id, id, content.title(), base.location(), content.label(), content.dialogue(),
				List.of(), true, content.effects(), content.objectives(), List.of(), QuestDelivery.MANUAL, null, null,
				null, true, List.of(), new QuestReception(base.location(), null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(value -> value.id().equals(id));
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		long owner = create();
		transactions.run(() -> inventoryCommands.grant(owner, "DELIVERY_WOOD", "LIFE_WOOD_0", 8, false));
		transactions.run(() -> inventoryCommands.grant(owner, "DELIVERY_ORE", "LIFE_ORE_0", 3, false));
		var state = accept(owner, id);
		var before = inventoryQueries.list(owner);
		long gold = queries.find(owner).gold();
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		assertEquals(before, inventoryQueries.list(owner));
		assertEquals(gold, queries.find(owner).gold());
		transactions.run(() -> inventoryCommands.grant(owner, "DELIVERY_ORE_LAST", "LIFE_ORE_0", 1, false));
		var delivery = request(state, QuestOperation.DELIVER, id);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(4,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_WOOD_0"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(0,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_ORE_0"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(gold + 30, queries.find(owner).gold());
		var settled = inventoryQueries.list(owner);
		runtime.command(owner, delivery);
		assertEquals(settled, inventoryQueries.list(owner));
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 正式装备教学要求新制造及当前持有同时成立，旧成品、历史事件都不能替代。 */
	@Test
	void equipmentCraftingRequiresNewProductionAndRetainedProduct() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		var content = catalog.step("N034");
		String id = "TEST_CRAFT_EQUIPMENT";
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, null);
		var base = catalog.step(id);
		var step = new QuestContentStep(id, id, content.title(), base.location(), content.label(), content.dialogue(),
				List.of(), true, content.effects(), content.objectives(), List.of(), QuestDelivery.MANUAL, null, null,
				null, true, List.of(), new QuestReception(base.location(), null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(value -> value.id().equals(id));
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		long owner = create();
		transactions.run(() -> {
			inventoryCommands.grant(owner, "OLD_SWORD", "CRAFT_1_SWORD", 1, true);
			facts.record(owner, "CRAFT_PRODUCT:CRAFT_1_SWORD");
		});
		var state = accept(owner, id);
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		// 制造服务真实发出事件已由LifeCraftingFlowTest覆盖；这里验证正式任务对事件种类和接取窗口的判断。
		transactions.run(() -> facts.record(owner, "CRAFT_PRODUCT:LIFE_PART_0"));
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		transactions.run(() -> facts.record(owner, "CRAFT_PRODUCT:CRAFT_1_SWORD"));
		var before = inventoryQueries.list(owner);
		var delivery = request(state, QuestOperation.DELIVER, id);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(before, inventoryQueries.list(owner));
		runtime.command(owner, delivery);
		assertEquals(before, inventoryQueries.list(owner));
		long withoutProduct = create();
		var missing = accept(withoutProduct, id);
		transactions.run(() -> facts.record(withoutProduct, "CRAFT_PRODUCT:CRAFT_1_SWORD"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(withoutProduct, request(missing, QuestOperation.DELIVER, id)));
	}

	/** 装备展示使用当前归属，重复交付不消费。 */
	@Test
	void equipmentHoldingRequiresOwnedEquipmentAndDoesNotConsumeIt() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		String id = "TEST_PRESENT_EQUIPMENT";
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, null);
		var base = catalog.step(id);
		var step = new QuestContentStep(id, id, "检查人物装备", base.location(), "检查装备", "检查完成", List.of(), true,
				base.effects(), List.of(new QuestObjective("owned", QuestMeasure.HOLDING, "ITEM:STARTER_WEAPON", 1)),
				List.of(), QuestDelivery.MANUAL, null, null, null, true, List.of(),
				new QuestReception(base.location(), null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(value -> value.id().equals(id));
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		long owner = create();
		long other = create();
		transactions.run(() -> inventoryCommands.grant(other, "OTHER_EQUIPMENT", "STARTER_WEAPON", 1, true));
		transactions.run(() -> inventoryCommands.grant(owner, "WRONG_ITEM", "HEALTH_POTION", 1, false));
		var state = accept(owner, id);
		assertEquals(0, runtime.views(owner).getLast().goals().getFirst().value());
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		transactions.run(() -> inventoryCommands.grant(owner, "OWN_EQUIPMENT", "STARTER_WEAPON", 1, true));
		assertEquals(1, runtime.views(owner).getLast().goals().getFirst().value());
		var before = inventoryQueries.list(owner);
		var delivery = request(state, QuestOperation.DELIVER, id);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(before, inventoryQueries.list(owner));
		runtime.command(owner, delivery);
		assertEquals(before, inventoryQueries.list(owner));
	}

	/** 使用正式赠予和编队入口验证展示，不把捕捉事件或前端状态伪造成当前所有权。 */
	@Autowired
	io.github.lishangbu.avalon.creature.application.command.CreatureCommandService creatures;

	@Autowired
	io.github.lishangbu.avalon.creature.application.query.CreatureQueryService creatureQueries;

	@Autowired
	io.github.lishangbu.avalon.creature.application.command.CreatureManagementService creatureManagement;

	@MockitoSpyBean
	QuestCatalog catalog;

	@MockitoSpyBean
	QuestRulesCatalog rules;

	@MockitoSpyBean
	QuestDialogueCatalog dialogues;

	@MockitoSpyBean
	QuestClock clock;

	/** 展示支持任务前获得、仓库存放、重复交付；其他角色的同种幻兽不能代替当前拥有。 */
	@Test
	void ownedCreaturePresentationIncludesStorageWithoutConsumingOrRequiringCapture() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		String id = "TEST_PRESENT_PET";
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, null);
		var base = catalog.step(id);
		var step = new QuestContentStep(id, id, "展示已有伙伴", base.location(), "展示伙伴（不消耗）", "展示完成", List.of(), true,
				base.effects(), List.of(new QuestObjective("owned", QuestMeasure.HOLDING, "PET:CLAW_CAT", 1)),
				List.of(), QuestDelivery.MANUAL, null, null, null, true, List.of(),
				new QuestReception(base.location(), null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(value -> value.id().equals(id));
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		long owner = create();
		long other = create();
		// 另一个角色拥有同种幻兽不满足条件；当前角色后续获得的赠予也无需捕捉事件。
		transactions.run(() -> creatures.grant(other, "PRESENT_OTHER", "CLAW_CAT"));
		// 接取前已有的赠予个体同样有效，证明展示不是“接取后重新捕获”的另一种写法。
		var existing = accept(other, id);
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(other, request(existing, QuestOperation.DELIVER, id)).instance().status());
		var state = accept(owner, id);
		assertEquals(0, runtime.views(owner).getLast().goals().getFirst().value());
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		transactions.run(() -> creatures.grant(owner, "PRESENT_GIFT", "CLAW_CAT"));
		var pet = creatureQueries.list(owner).getFirst();
		// 赠予的默认利爪猫为3级；先正常提升人物，避免仓储准备被“伙伴等级高于人物”拒绝。
		transactions.run(() -> characters.grantExperience(owner, 10000));
		assertTrue(queries.find(owner).level() >= pet.level());
		// 沿正式相邻道路到中心，先调至携带位再存仓，使用真实仓储规则而非伪造-1位置。
		for (String place : List.of("STONE_PATH", "SQUARE", "PET_CENTER"))
			characters.move(owner, place, queries.find(owner).revision());
		creatures.position(owner, Long.parseLong(pet.id()), 2);
		creatureManagement.deposit(owner, Long.parseLong(pet.id()));
		for (String place : List.of("SQUARE", "STONE_PATH", "VILLAGE_HOME"))
			characters.move(owner, place, queries.find(owner).revision());
		var before = creatureQueries.list(owner);
		assertEquals(1, runtime.views(owner).getLast().goals().getFirst().value());
		var request = request(state, QuestOperation.DELIVER, id);
		var settled = runtime.command(owner, request);
		assertEquals(QuestStatus.COMPLETED, settled.instance().status());
		assertEquals(before, creatureQueries.list(owner));
		assertEquals(settled, runtime.command(owner, request));
		assertEquals(before, creatureQueries.list(owner));
	}

	/** 指定物种捕捉只累计新捕获，历史捕获、赠予、其他物种及来源重试都不能冒充进度。 */
	@Test
	void speciesCaptureUsesNewCaptureEventsAndIgnoresSourceReplay() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		String id = "TEST_CAPTURE_SPECIES";
		configure(id, QuestRepeatPolicy.ONCE, 1, 2, null);
		var base = catalog.step(id);
		var step = new QuestContentStep(id, id, "捕捉利爪猫", base.location(), "确认成果", "已完成", List.of(), true,
				base.effects(), List.of(new QuestObjective("capture", QuestMeasure.EVENT, "CAPTURE:CLAW_CAT", 2)),
				List.of(), QuestDelivery.MANUAL, null, null, null, true, List.of(),
				new QuestReception(base.location(), null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(value -> value.id().equals(id));
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		long owner = create();
		transactions.run(() -> creatures.capture(owner, "SPECIES_OLD", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹")));
		var state = accept(owner, id);
		transactions.run(() -> creatures.grant(owner, "SPECIES_GIFT", "CLAW_CAT"));
		transactions.run(() -> creatures.capture(owner, "SPECIES_OLD", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹")));
		transactions.run(() -> creatures.capture(owner, "SPECIES_OTHER", "SPROUT", 1, "A", "完好防护", List.of("藤鞭")));
		assertEquals(0, runtime.views(owner).getLast().goals().getFirst().value());
		transactions.run(() -> creatures.capture(owner, "SPECIES_NEW", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹")));
		transactions.run(() -> creatures.capture(owner, "SPECIES_NEW", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹")));
		assertEquals(1, runtime.views(owner).getLast().goals().getFirst().value());
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		transactions.run(() -> creatures.capture(owner, "SPECIES_NEW_TWO", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹")));
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(state, QuestOperation.DELIVER, id)).instance().status());
	}

	/** 每日任务跨周期开启新实例，旧READY资格保留；重复交付请求完整重放且不会再次发金币。 */
	@Test
	void dailyInstancesAndRequestReplaySurviveTransactionBoundaries() {
		// 真实接取事实由系统时钟生成；模拟任务时钟必须始终在其之后。
		// 取服务器时区明天的23:59，避免运行日期超过固定日期后被判定为未来事件。
		var zone = ZoneId.of("Asia/Hong_Kong");
		Instant today = Instant.now().atZone(zone).toLocalDate().plusDays(1).atTime(23, 59).atZone(zone).toInstant();
		doReturn(today).when(clock).now();
		configure("TEST_DAILY", QuestRepeatPolicy.DAILY, 1, 2, null);
		long owner = create();
		var first = accept(owner, "TEST_DAILY");
		record(owner);
		record(owner);
		var ready = runtime.state(owner).getLast();
		assertEquals(QuestStatus.READY, ready.status());
		doReturn(today.plusSeconds(60)).when(clock).now();
		var request = request(ready, QuestOperation.DELIVER, "TEST_DAILY");
		long before = queries.find(owner).gold();
		var settled = runtime.command(owner, request);
		assertEquals(QuestStatus.COMPLETED, settled.instance().status());
		assertEquals(before + 7, queries.find(owner).gold());
		assertEquals(settled, runtime.command(owner, request));
		assertEquals(before + 7, queries.find(owner).gold());
		var second = accept(owner, "TEST_DAILY");
		assertNotEquals(first.id(), second.id());
		assertTrue(second.progress().isEmpty());
		assertThrows(RuntimeException.class, () -> runtime.command(create(), request));
		assertThrows(RuntimeException.class, () -> runtime.command(owner, new QuestInstanceRequest(request.requestId(),
				second.id(), second.round(), request.operation(), request.step(), null, null)));
	}

	/** 真正的回执约束失败会回滚金币和实例完成状态，相同请求号修复后可以安全重试。 */
	@Test
	void receiptFailureRollsBackAssetsAndInstanceTogether() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		configure("TEST_ATOMIC", QuestRepeatPolicy.ONCE, 1, 1, null);
		long owner = create();
		var state = accept(owner, "TEST_ATOMIC");
		record(owner);
		var request = request(state, QuestOperation.DELIVER, "TEST_ATOMIC");
		long before = queries.find(owner).gold();
		jdbc.execute("alter table quest_runtime_request add constraint test_runtime_failure check (false) not valid");
		try {
			assertThrows(RuntimeException.class, () -> runtime.command(owner, request));
		}
		finally {
			jdbc.execute("alter table quest_runtime_request drop constraint test_runtime_failure");
		}
		assertEquals(before, queries.find(owner).gold());
		assertEquals(QuestStatus.READY, runtime.state(owner).getLast().status());
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, request).instance().status());
		assertEquals(before + 7, queries.find(owner).gold());
	}

	/** 环内第二轮拒绝第一轮旧命令；放弃不返还开启次数，次日可创建从第一轮开始的新实例。 */
	@Test
	void ringRoundAndDailyOpeningQuotaAreEnforcedByHost() {
		var today = Instant.now().plusSeconds(60);
		doReturn(today).when(clock).now();
		configure("TEST_RING", QuestRepeatPolicy.RING, 1, 1, null);
		long owner = create();
		var first = accept(owner, "TEST_RING");
		record(owner);
		var request = request(first, QuestOperation.DELIVER, "TEST_RING");
		var next = runtime.command(owner, request).instance();
		assertEquals(2, next.round());
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(), first.id(), 1,
						QuestOperation.DELIVER, "TEST_RING", null, null)));
		runtime.command(owner, request(next, QuestOperation.ABANDON, null));
		assertThrows(RuntimeException.class, () -> accept(owner, "TEST_RING"));
		doReturn(today.plusSeconds(86400)).when(clock).now();
		var restarted = accept(owner, "TEST_RING");
		assertEquals(1, restarted.round());
		assertNotEquals(first.id(), restarted.id());
	}

	/** 任务超时后重试清空计数，并且使用新的实例与完整计时窗口。 */
	@Test
	void timeoutCreatesFreshAttempt() {
		var start = Instant.now().plusSeconds(60);
		doReturn(start).when(clock).now();
		configure("TEST_TIMED", QuestRepeatPolicy.ONCE, 1, 2, new QuestTimeout(30L, null));
		long owner = create();
		var first = accept(owner, "TEST_TIMED");
		record(owner);
		doReturn(start.plusSeconds(30)).when(clock).now();
		var failed = runtime.command(owner, request(first, QuestOperation.REFRESH, null)).instance();
		assertEquals(QuestStatus.FAILED, failed.status());
		var retry = accept(owner, "TEST_TIMED");
		assertNotEquals(first.id(), retry.id());
		assertTrue(retry.progress().isEmpty());
	}

	/** 已交付的旧步骤保留历史奖励，但不能阻止限时新尝试从入口重新开始。 */
	@Test
	void timedRetryCanRestartPreviouslyDeliveredEntry() {
		var start = Instant.now().plusSeconds(60);
		doReturn(start).when(clock).now();
		String id = "TEST_TIMED_STAGES";
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, new QuestTimeout(30L, null));
		var entry = catalog.step(id);
		String nextId = id + "_NEXT";
		var next = new QuestContentStep(nextId, id, entry.title(), entry.location(), entry.label(), entry.dialogue(),
				List.of(id), true, entry.effects(), entry.objectives(), entry.conditions(), entry.delivery(),
				entry.branch(), entry.timeout(), null, true, List.of(), new QuestReception(entry.location(), null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.add(next);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(next).when(catalog).step(nextId);
		doReturn(dialogues.step("O001")).when(dialogues).step(nextId);
		long owner = create();
		var first = accept(owner, id);
		record(owner);
		var advanced = runtime.command(owner, request(first, QuestOperation.DELIVER, id)).instance();
		assertTrue(advanced.completed().contains(id));
		doReturn(start.plusSeconds(30)).when(clock).now();
		var retry = accept(owner, id);
		assertNotEquals(first.id(), retry.id());
		assertTrue(retry.completed().isEmpty());
		record(owner);
		var secondAdvance = runtime.command(owner, request(retry, QuestOperation.DELIVER, id)).instance();
		assertTrue(secondAdvance.completed().contains(id));
		accept(owner, nextId);
	}

	/** 分支选择、公共汇合、日志完成数与剧情回执使用同一实例规则，未选路线不需要交付。 */
	@Test
	void branchSelectionCompletesOnlyChosenRouteAndCommonJoin() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		String id = "TEST_BRANCH";
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, null);
		var base = catalog.step(id);
		var a = branchStep(base, id, List.of(), new QuestBranch("route", "A"), QuestDelivery.MANUAL);
		var b = branchStep(base, id + "_B", List.of(), new QuestBranch("route", "B"), QuestDelivery.MANUAL);
		var join = branchStep(base, id + "_JOIN", List.of(a.id(), b.id()), null, QuestDelivery.AUTO);
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(step -> step.quest().equals(id));
		all.addAll(List.of(a, b, join));
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		for (var step : List.of(a, b, join))
			doReturn(step).when(catalog).step(step.id());
		long owner = create();
		var state = accept(owner, id);
		assertEquals(2, runtime.views(owner).getLast().branches().size());
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, a.id())));
		var chosen = runtime
			.command(owner,
					new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), 1, QuestOperation.CHOOSE, null,
							"route", "B"))
			.instance();
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), 1,
						QuestOperation.CHOOSE, null, "route", "A")));
		var complete = runtime.command(owner, request(chosen, QuestOperation.DELIVER, b.id())).instance();
		assertEquals(QuestStatus.COMPLETED, complete.status());
		assertFalse(complete.completed().contains(a.id()));
		assertTrue(questQueries.done(owner, join.id()));
		var entry = questQueries.state(owner)
			.journal()
			.stream()
			.filter(value -> value.id().equals(id))
			.findFirst()
			.orElseThrow();
		assertTrue(entry.completed());
		assertEquals(2, entry.totalSteps());
		assertEquals(2, entry.completedSteps());
	}

	/** SWITCHABLE方法在完成前可改选，切换后旧路关闭，完成新路后服务器移除改选入口。 */
	@Test
	void switchableMethodCanChangeUntilOneRouteCompletes() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		String id = "TEST_SWITCHABLE";
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, null);
		var base = catalog.step(id);
		var first = switchableStep(base, id, "A");
		var second = switchableStep(base, id + "_B", "B");
		var join = branchStep(base, id + "_JOIN", List.of(first.id(), second.id()), null, QuestDelivery.MANUAL);
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(step -> step.quest().equals(id));
		all.addAll(List.of(first, second, join));
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		for (var step : List.of(first, second, join))
			doReturn(step).when(catalog).step(step.id());
		long owner = create();
		transactions.run(() -> {
			inventoryCommands.grant(owner, "OR_POTIONS", "HEALTH_POTION", 3, false);
			inventoryCommands.grant(owner, "OR_HERBS", "LIFE_HERB_0", 6, false);
		});
		var state = accept(owner, id);
		var selected = runtime
			.command(owner,
					new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), 1, QuestOperation.CHOOSE, null,
							"method", "A"))
			.instance();
		assertEquals(2, runtime.views(owner).getLast().branches().size());
		selected = runtime
			.command(owner,
					new QuestInstanceRequest(UUID.randomUUID().toString(), selected.id(), 1, QuestOperation.CHOOSE,
							null, "method", "B"))
			.instance();
		assertEquals("B", selected.choices().get("method"));
		assertEquals(2, runtime.views(owner).getLast().branches().size());
		var beforeDelivery = selected;
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(beforeDelivery, QuestOperation.DELIVER, first.id())));
		selected = runtime.command(owner, request(selected, QuestOperation.DELIVER, second.id())).instance();
		assertEquals(3,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("HEALTH_POTION"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(0,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_HERB_0"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(QuestStatus.READY, selected.status());
		assertTrue(runtime.views(owner).getLast().branches().isEmpty());
		var completedMethod = selected;
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(),
						completedMethod.id(), completedMethod.round(), QuestOperation.CHOOSE, null, "method", "A")));
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(selected, QuestOperation.DELIVER, join.id())).instance().status());
	}

	private QuestContentStep switchableStep(QuestContentStep base, String id, String option) {
		var source = catalog.step(option.equals("A") ? "N026_POTIONS" : "N026_HERBS");
		return new QuestContentStep(id, base.quest(), "可切换方法" + option, base.location(), base.label(), base.dialogue(),
				List.of(), false, source.effects(), source.objectives(), List.of(), QuestDelivery.MANUAL,
				new QuestBranch("method", option, io.github.lishangbu.avalon.quest.model.QuestBranchMode.SWITCHABLE),
				null, null, true, List.of(), new QuestReception(base.location(), null));
	}

	/** 使用正式工坊两方案验证各自消耗、互斥选择及共同汇合，不能只靠任务数量证明分支落地。 */
	@Test
	void workshopChoicesConsumeOnlySelectedMaterialsAndReachCommonJoin() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		var repair = catalog.step("N053_REPAIR");
		var build = catalog.step("N053_BUILD");
		for (String option : List.of("REPAIR", "BUILD")) {
			String id = "TEST_WORKSHOP_" + option;
			configure(id, QuestRepeatPolicy.ONCE, 1, 1, null);
			var base = catalog.step(id);
			var routes = new ArrayList<QuestContentStep>();
			for (var source : List.of(repair, build)) {
				String routeId = source == repair ? id : id + "_BUILD";
				routes.add(new QuestContentStep(routeId, id, source.title(), base.location(), source.label(),
						source.dialogue(), List.of(), false, source.effects(), source.objectives(), List.of(),
						QuestDelivery.MANUAL, source.branch(), null, null, true, List.of(),
						new QuestReception(base.location(), null)));
			}
			var join = branchStep(base, id + "_JOIN", routes.stream().map(QuestContentStep::id).toList(), null,
					QuestDelivery.AUTO);
			var all = new ArrayList<>(catalog.definition().steps());
			all.removeIf(step -> step.quest().equals(id));
			all.addAll(routes);
			all.add(join);
			doReturn(new QuestContentDefinition(all)).when(catalog).definition();
			for (var step : routes)
				doReturn(step).when(catalog).step(step.id());
			doReturn(join).when(catalog).step(join.id());
			long owner = create();
			transactions.run(() -> {
				inventoryCommands.grant(owner, "WORKSHOP_WOOD", "LIFE_WOOD_0", 4, false);
				inventoryCommands.grant(owner, "WORKSHOP_ORE", "LIFE_ORE_0", 4, false);
			});
			var state = accept(owner, id);
			assertEquals(2, runtime.views(owner).getLast().branches().size());
			var chosen = runtime
				.command(owner,
						new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), 1, QuestOperation.CHOOSE,
								null, "NOM_WORKSHOP", option))
				.instance();
			int index = option.equals("REPAIR") ? 0 : 1;
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(chosen, QuestOperation.DELIVER, routes.get(1 - index).id())));
			var delivered = runtime.command(owner, request(chosen, QuestOperation.DELIVER, routes.get(index).id()))
				.instance();
			assertEquals(QuestStatus.COMPLETED, delivered.status());
			assertTrue(questQueries.done(owner, join.id()));
			String consumed = option.equals("REPAIR") ? "LIFE_WOOD_0" : "LIFE_ORE_0";
			String retained = option.equals("REPAIR") ? "LIFE_ORE_0" : "LIFE_WOOD_0";
			assertEquals(0,
					inventoryQueries.list(owner)
						.stream()
						.filter(item -> item.code().equals(consumed))
						.mapToInt(item -> item.quantity())
						.sum());
			assertEquals(4,
					inventoryQueries.list(owner)
						.stream()
						.filter(item -> item.code().equals(retained))
						.mapToInt(item -> item.quantity())
						.sum());
		}
	}

	/** 同组两条互斥路线加公共汇合，所有显示文本仍来自游戏内容而非核心编号。 */
	private QuestContentStep branchStep(QuestContentStep base, String id, List<String> requires, QuestBranch branch,
			QuestDelivery delivery) {
		return new QuestContentStep(id, base.quest(), "路线" + id, base.location(), base.label(), base.dialogue(),
				requires, true, List.of(new QuestEffect("GOLD", null, 7)), List.of(), List.of(), delivery, branch, null,
				null, true, List.of(), new QuestReception(base.location(), null));
	}

	/** 同一微秒完成多次每日任务仍以尝试序号定位最新实例，并准确限制次数。 */
	@Test
	void sameTimestampAttemptsRemainOrderedAndRespectQuota() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		configure("TEST_MULTI_DAILY", QuestRepeatPolicy.DAILY, 2, 1, null);
		long owner = create();
		var first = accept(owner, "TEST_MULTI_DAILY");
		record(owner);
		runtime.command(owner, request(first, QuestOperation.DELIVER, "TEST_MULTI_DAILY"));
		var second = accept(owner, "TEST_MULTI_DAILY");
		assertNotEquals(first.id(), second.id());
		assertEquals(second.id(), runtime.latest(queries.requireId(owner), "TEST_MULTI_DAILY").id());
		assertTrue(second.progress().isEmpty());
		record(owner);
		runtime.command(owner, request(second, QuestOperation.DELIVER, "TEST_MULTI_DAILY"));
		assertThrows(RuntimeException.class, () -> accept(owner, "TEST_MULTI_DAILY"));
		assertTrue(questQueries.state(owner)
			.available()
			.stream()
			.noneMatch(value -> value.quest().equals("TEST_MULTI_DAILY")));
	}

	/** 同时刻新尝试不能复用旧补给资格，环内每轮补给独立且同轮重接保持幂等。 */
	@Test
	void suppliesAreScopedToAttemptAndRingRound() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		String id = "TEST_RING_SUPPLIES";
		configure(id, QuestRepeatPolicy.RING, 1, 1, null);
		var base = catalog.step(id);
		var step = new QuestContentStep(id, id, base.title(), "VILLAGE_HOME", base.label(), base.dialogue(), List.of(),
				true, base.effects(), base.objectives(), base.conditions(), base.delivery(), base.branch(),
				base.timeout(), null, true, List.of(new QuestEffect("GIVE_ITEM", "HEALTH_POTION", 2)),
				new QuestReception("VILLAGE_HOME", null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(value -> value.id().equals(id));
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		long owner = create();
		var first = accept(owner, id);
		record(owner);
		var second = runtime.command(owner, request(first, QuestOperation.DELIVER, id)).instance();
		assertEquals(2, second.round());
		record(owner);
		// 下一轮即使已经完成击杀，也须接取本轮补给后才能交付。
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(second, QuestOperation.DELIVER, id)));
		var grant = acceptance.accept(owner, id,
				new QuestAcceptRequest(UUID.randomUUID().toString(), queries.find(owner).revision()));
		assertEquals(1, grant.rewards().size());
		var repeated = acceptance.accept(owner, id,
				new QuestAcceptRequest(UUID.randomUUID().toString(), queries.find(owner).revision()));
		assertTrue(repeated.rewards().isEmpty());
		assertEquals(3, runtime.command(owner, request(second, QuestOperation.DELIVER, id)).instance().round());
	}

	/** 测试内容使用生产扩展字段；旧地点与NPC引用仍保留正式校验语义。 */
	private void configure(String id, QuestRepeatPolicy repeat, int limit, int count, QuestTimeout timeout) {
		var step = new QuestContentStep(id, id, "测试任务", "VILLAGE_HOME", "交付", "已完成", List.of(), true,
				List.of(new QuestEffect("GOLD", null, 7)),
				List.of(new QuestObjective("kills", QuestMeasure.EVENT, "KILL:TEST", count)), List.of(),
				QuestDelivery.MANUAL, null, null, null, true, List.of(), new QuestReception("VILLAGE_HOME", null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		doReturn(dialogues.step("O001")).when(dialogues).step(id);
		doReturn(new QuestRule(id, "测试任务", QuestType.SIDE, List.of(), true,
				new QuestPolicy(repeat, "Asia/Hong_Kong", limit, 10, timeout, true,
						List.of(new QuestEffect("GOLD", null, 100))
							.stream()
							.filter(effect -> repeat == QuestRepeatPolicy.RING)
							.toList())))
			.when(rules)
			.rule(id); // 日志从整份规则目录聚合，临时任务也加入完整定义而非只替换单条查询。
		var allRules = new ArrayList<>(rules.definition().quests());
		allRules.add(rules.rule(id));
		doReturn(new QuestRulesDefinition(rules.definition().tags(), allRules)).when(rules).definition();

	}

	/** 固定任务时钟下发布带相同时刻的真实服务器事件，避免测试机器日期影响截止断言。 */
	private void record(long owner) {
		transactions.run(
				() -> runtime.record(owner, new QuestEvent(UUID.randomUUID().toString(), "KILL:TEST", 1, clock.now())));
	}

	private long create() {
		String name = "qr" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		registration.register(name, "Quest-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		return owner;
	}

	private QuestInstance acceptOpening(long owner, String step) {
		acceptance.accept(owner, step,
				new QuestAcceptRequest(UUID.randomUUID().toString(), queries.find(owner).revision()));
		return runtime.state(owner)
			.stream()
			.filter(state -> state.quest().equals(catalog.step(step).quest()))
			.findFirst()
			.orElseThrow();
	}

	private QuestInstance accept(long owner, String step) {
		acceptance.accept(owner, step,
				new QuestAcceptRequest(UUID.randomUUID().toString(), queries.find(owner).revision()));
		return runtime.state(owner).getLast();
	}

	private QuestInstanceRequest request(QuestInstance state, QuestOperation operation, String step) {
		return new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), state.round(), operation, step, null,
				null);
	}

}
