package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.quest.model.QuestStatus;
import io.github.lishangbu.avalon.world.quest.model.QuestInstanceRequest;
import io.github.lishangbu.avalon.world.quest.model.QuestOperation;
import java.time.Instant;
import java.util.List;
import io.github.lishangbu.avalon.quest.model.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 水城与风城的双城顺序、深层路线及交付约束回归。 */
class QuestMiddleCitiesRuntimeTest extends QuestRuntimeTestSupport {

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

}
