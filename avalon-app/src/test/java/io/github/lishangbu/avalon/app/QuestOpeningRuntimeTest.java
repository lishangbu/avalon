package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.quest.model.QuestStatus;
import io.github.lishangbu.avalon.world.quest.model.QuestOperation;
import java.time.Instant;
import java.util.List;
import io.github.lishangbu.avalon.quest.model.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 四职业从新账号开始的自然开篇与重复结算回归。 */
class QuestOpeningRuntimeTest extends QuestRuntimeTestSupport {

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

}
