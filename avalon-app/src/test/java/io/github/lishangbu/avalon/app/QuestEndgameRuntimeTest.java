package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.quest.model.*;
import io.github.lishangbu.avalon.quest.model.QuestStatus;
import io.github.lishangbu.avalon.world.application.model.SceneActionRequest;
import io.github.lishangbu.avalon.world.quest.model.QuestOperation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 联合章节、终局入口和后日谈的真实事务回归，保留四城衔接断言。 */
class QuestEndgameRuntimeTest extends QuestRuntimeTestSupport {

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
		var action = new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null);
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
		action = new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null);
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
	void jointEpilogueAndChallengeRequireNewVisitsVictoriesAndCrafting() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("U025", "S025", "J025", "J026"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", "SALAMANDER_CITY", queries.requireId(owner));
		for (String id : List.of("J027", "J028", "J029", "J030", "E006", "E007", "E008", "E010", "E009")) {
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
		assertThrows(RuntimeException.class, () -> sceneCommands.execute(owner, "J011_RECOVER",
				new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null)));
		openingMove(owner, "SALAMANDER_CITY");
		var record = acceptOpening(owner, "J011");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(record, QuestOperation.DELIVER, "J011")));
		openingMove(owner, "J_CORE_PERIMETER");
		var action = new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null);
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
		action = new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null);
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

}
