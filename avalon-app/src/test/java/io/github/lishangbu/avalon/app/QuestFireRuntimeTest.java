package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.quest.model.*;
import io.github.lishangbu.avalon.quest.model.QuestStatus;
import io.github.lishangbu.avalon.world.application.model.SceneActionRequest;
import io.github.lishangbu.avalon.world.quest.model.QuestInstanceRequest;
import io.github.lishangbu.avalon.world.quest.model.QuestOperation;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 火城主线、制造、捕捉与可切换准备路线的真实事务回归。 */
class QuestFireRuntimeTest extends QuestRuntimeTestSupport {

	@ParameterizedTest
	@ValueSource(strings = { "U025", "S025" })
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
				new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null));
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
				var operation = new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(),
						null);
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

	@ParameterizedTest
	@ValueSource(strings = { "FIGHT", "REPAIR" })
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
		var operation = new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		var result = sceneCommands.execute(owner, "F014_SHUTDOWN", operation);
		assertEquals(result, sceneCommands.execute(owner, "F014_SHUTDOWN", operation));
		openingMove(owner, "SALAMANDER_SEALED_WORKS", "SALAMANDER_ASH_ROAD", "SALAMANDER_CITY");
		runtime.command(owner, request(shutdown, QuestOperation.DELIVER, "F014"));
		var unlock = acceptOpening(owner, "F015");
		openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_SEALED_WORKS", "F_CORE_APPROACH");
		sceneCommands.execute(owner, "F015_UNLOCK",
				new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null));
		openingMove(owner, "SALAMANDER_SEALED_WORKS", "SALAMANDER_ASH_ROAD", "SALAMANDER_CITY");
		var finalDelivery = request(unlock, QuestOperation.DELIVER, "F015");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, finalDelivery).instance().status());
		xp = queries.find(owner).experience();
		runtime.command(owner, finalDelivery);
		assertEquals(xp, queries.find(owner).experience());
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

	@ParameterizedTest
	@ValueSource(strings = { "SAFE", "FIELD" })
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
				new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null));
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
		for (var expected : Map.of("F_FURNACE_FRAGMENT", 1, "F_FURNACE_BODY", 1).entrySet())
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
		long threshold = LifeSkillRules.threshold(7);
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
		for (var expected : Map.of("LIFE_ORE_3", 2, "F_HEAT_RESISTANT_HIDE", 1, "CRAFT_7_BODY", 1).entrySet())
			assertEquals(expected.getValue().intValue(),
					inventoryQueries.list(owner)
						.stream()
						.filter(item -> item.code().equals(expected.getKey()))
						.mapToInt(item -> item.quantity())
						.sum());
	}

	@ParameterizedTest
	@ValueSource(ints = { 31, 36 })
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
		for (var expected : Map.of("LIFE_ORE_3", 2, "LIFE_PART_3", 1, "TOOL_SILVER_PICKAXE", 1).entrySet())
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
		assertThrows(RuntimeException.class, () -> sceneCommands.execute(owner, "F019_UNLOCK",
				new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null)));
		openingMove(owner, "SALAMANDER_ASH_ROAD", "SALAMANDER_SEALED_WORKS", "F_CORE_APPROACH", "F_SEAL_FORECOURT");
		var operation = new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null);
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
		var recovery = new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null);
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

}
