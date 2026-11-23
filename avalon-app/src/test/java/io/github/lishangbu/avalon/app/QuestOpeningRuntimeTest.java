package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.DungeonRunCommandService;
import io.github.lishangbu.avalon.battle.application.query.DungeonCombatQueryService;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundStateSubmission;
import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import io.github.lishangbu.avalon.quest.model.*;
import io.github.lishangbu.avalon.quest.model.QuestStatus;
import io.github.lishangbu.avalon.world.application.command.LifeShopService;
import io.github.lishangbu.avalon.world.application.command.LifeSkillCommandService;
import io.github.lishangbu.avalon.world.application.model.LifeShopRequest;
import io.github.lishangbu.avalon.world.application.model.LifeSkillRequest;
import io.github.lishangbu.avalon.world.application.model.SceneActionRequest;
import io.github.lishangbu.avalon.world.quest.model.QuestInstanceRequest;
import io.github.lishangbu.avalon.world.quest.model.QuestOperation;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 四职业从新账号开始的自然开篇与重复结算回归。 */
class QuestOpeningRuntimeTest extends QuestRuntimeTestSupport {

	@Autowired
	LifeShopService lifeShop;

	@Autowired
	LifeSkillCommandService lifeLearning;

	@Autowired
	DungeonRunCommandService dungeonRuns;

	@Autowired
	DungeonCombatQueryService dungeonCombat;

	/** 四职业从新角色实际行走完成开篇准备，不注入资产或剧情事实。 */
	@ParameterizedTest
	@ValueSource(strings = { "WARRIOR", "RANGER", "MAGE", "PRIEST" })
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
				new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null));
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
				List.of(new BattleStateOrder("trainee", "DEFEND", "trainee"),
						new BattleStateOrder("T_CAT", "SWAP", "T_SPROUT"),
						new BattleStateOrder("T_STONE", "DEFEND", "T_STONE")),
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
		for (var skill : List.of(LifeSkillRulesSkill.LEATHERWORK, LifeSkillRulesSkill.HUNTING,
				LifeSkillRulesSkill.HERBALISM))
			lifeLearning.change(owner, new LifeSkillRequest(UUID.randomUUID().toString(), skill, false), false);
		var support = acceptOpening(owner, "N003");
		int ore = inventoryQueries.list(owner)
			.stream()
			.filter(item -> item.code().equals("LIFE_ORE_0"))
			.mapToInt(item -> item.quantity())
			.sum();
		int shortage = Math.max(0, 6 - ore);
		long beforePurchase = queries.find(owner).gold();
		if (shortage > 0) {
			var purchase = new LifeShopRequest(UUID.randomUUID().toString(), "LIFE_ORE_0", shortage);
			lifeShop.buy(owner, purchase);
			lifeShop.buy(owner, purchase);
		}
		assertEquals(beforePurchase - shortage * 8, queries.find(owner).gold());
		var supportDelivery = request(support, QuestOperation.DELIVER, "N003");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, supportDelivery).instance().status());
		assertEquals(Math.max(0, ore - 6),
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_ORE_0"))
					.mapToInt(item -> item.quantity())
					.sum());
		long afterDelivery = queries.find(owner).gold();
		runtime.command(owner, supportDelivery);
		assertEquals(afterDelivery, queries.find(owner).gold());
		var beasts = acceptOpening(owner, "N002");
		assertThrows(RuntimeException.class, () -> acceptOpening(owner, "N004_REPAIR"));
		for (int i = 0; i < 3; i++) {
			openingMove(owner, "GNOME_CITY", "CITY_PETS");
			openingBattles.recover(owner);
			openingMove(owner, "GNOME_CITY", "CITY_OUTSKIRTS", "NOM_MINE_APPROACH");
			openingFight(owner, "NOM_MINE_BEASTS");
			openingMove(owner, "CITY_OUTSKIRTS", "GNOME_CITY", "CITY_SHOP");
			if (i < 2)
				assertThrows(RuntimeException.class,
						() -> runtime.command(owner, request(beasts, QuestOperation.DELIVER, "N002")));
		}
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(beasts, QuestOperation.DELIVER, "N002")).instance().status());
		var route = acceptOpening(owner, "N004_REPAIR");
		runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(), route.id(), route.round(),
				QuestOperation.CHOOSE, null, "NOM_MINE_ROUTE", "REPAIR"));
		int wood = inventoryQueries.list(owner)
			.stream()
			.filter(item -> item.code().equals("LIFE_WOOD_0"))
			.mapToInt(item -> item.quantity())
			.sum();
		int woodShortage = Math.max(0, 4 - wood);
		long beforeWood = queries.find(owner).gold();
		if (woodShortage > 0)
			lifeShop.buy(owner, new LifeShopRequest(UUID.randomUUID().toString(), "LIFE_WOOD_0", woodShortage));
		assertEquals(beforeWood - woodShortage * 8, queries.find(owner).gold());
		var routeDelivery = request(route, QuestOperation.DELIVER, "N004_REPAIR");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, routeDelivery).instance().status());
		assertTrue(questQueries.done(owner, "N004"));
		assertEquals(beforeWood - woodShortage * 8 + 30, queries.find(owner).gold());
		assertEquals(Math.max(0, wood - 4),
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_WOOD_0"))
					.mapToInt(item -> item.quantity())
					.sum());
		runtime.command(owner, routeDelivery);
		assertEquals(beforeWood - woodShortage * 8 + 30, queries.find(owner).gold());
		assertEquals(QuestStatus.ACTIVE, acceptOpening(owner, "N005").status());
		for (int i = 0; i < 2; i++) {
			openingMove(owner, "GNOME_CITY", "CITY_PETS");
			openingBattles.recover(owner);
			openingMove(owner, "GNOME_CITY", "CITY_OUTSKIRTS", "NOM_MINE_APPROACH", "NOM_MINE_PASSAGE");
			openingFight(owner, "NOM_TUNNEL_PATROL");
			assertFalse(questQueries.done(owner, "N005"));
			if (i == 0)
				openingMove(owner, "NOM_MINE_APPROACH", "CITY_OUTSKIRTS", "GNOME_CITY", "CITY_SHOP");
		}
		long beforeArrivalGold = queries.find(owner).gold();
		long beforeArrivalXp = queries.find(owner).experience();
		openingMove(owner, "NOM_ABANDONED_POST");
		assertTrue(questQueries.done(owner, "N005"));
		assertEquals(beforeArrivalGold + 30, queries.find(owner).gold());
		assertEquals(beforeArrivalXp + 5000, queries.find(owner).experience());
		openingMove(owner, "NOM_MINE_PASSAGE", "NOM_ABANDONED_POST");
		assertEquals(beforeArrivalGold + 30, queries.find(owner).gold());
		assertEquals(beforeArrivalXp + 5000, queries.find(owner).experience());
		openingMove(owner, "NOM_MINE_PASSAGE", "NOM_MINE_APPROACH", "CITY_OUTSKIRTS", "GNOME_CITY", "CITY_SHOP");
		var post = acceptOpening(owner, "N006");
		openingMove(owner, "GNOME_CITY", "CITY_PETS");
		openingBattles.recover(owner);
		int extraPatrols = 0;
		while (queries.find(owner).level() < 10) {
			assertTrue(extraPatrols++ < 30, "哨所前补级不应超过30场通道巡查");
			openingMove(owner, "GNOME_CITY", "CITY_OUTSKIRTS", "NOM_MINE_APPROACH", "NOM_MINE_PASSAGE");
			openingFight(owner, "NOM_TUNNEL_PATROL");
			openingMove(owner, "NOM_MINE_APPROACH", "CITY_OUTSKIRTS", "GNOME_CITY", "CITY_PETS");
			openingBattles.recover(owner);
		}
		openingMove(owner, "GNOME_CITY", "CITY_OUTSKIRTS", "NOM_MINE_APPROACH", "NOM_MINE_PASSAGE",
				"NOM_ABANDONED_POST");
		openingFight(owner, "NOM_POST_GUARD");
		var recoverRelic = new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		var recoveredRelic = sceneCommands.execute(owner, "N006_RECOVER", recoverRelic);
		assertEquals(recoveredRelic, sceneCommands.execute(owner, "N006_RECOVER", recoverRelic));
		openingMove(owner, "NOM_MINE_PASSAGE", "NOM_MINE_APPROACH", "CITY_OUTSKIRTS", "GNOME_CITY", "CITY_SHOP");
		long beforeRelicGold = queries.find(owner).gold();
		long beforeRelicXp = queries.find(owner).experience();
		var postDelivery = request(post, QuestOperation.DELIVER, "N006");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, postDelivery).instance().status());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("NOM_POST_RELIC")));
		runtime.command(owner, postDelivery);
		assertEquals(beforeRelicGold + 30, queries.find(owner).gold());
		assertEquals(beforeRelicXp + 600, queries.find(owner).experience());
		var fragments = acceptOpening(owner, "N007");
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("NOM_GUARD_FRAGMENT"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(fragments, QuestOperation.DELIVER, "N007")));
		for (int i = 0; i < 2; i++) {
			openingMove(owner, "GNOME_CITY", "CITY_PETS");
			openingBattles.recover(owner);
			openingMove(owner, "GNOME_CITY", "CITY_OUTSKIRTS", "NOM_MINE_APPROACH", "NOM_MINE_PASSAGE",
					"NOM_ABANDONED_POST");
			openingFight(owner, "NOM_POST_GUARD");
			openingMove(owner, "NOM_MINE_PASSAGE", "NOM_MINE_APPROACH", "CITY_OUTSKIRTS", "GNOME_CITY", "CITY_SHOP");
		}
		long beforeFragments = queries.find(owner).gold();
		var fragmentDelivery = request(fragments, QuestOperation.DELIVER, "N007");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, fragmentDelivery).instance().status());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("NOM_GUARD_FRAGMENT")));
		runtime.command(owner, fragmentDelivery);
		assertEquals(beforeFragments + 30, queries.find(owner).gold());
		var gatekeeper = acceptOpening(owner, "N008");
		openingMove(owner, "GNOME_CITY", "CITY_PETS");
		openingBattles.recover(owner);
		openingMove(owner, "GNOME_CITY", "CITY_OUTSKIRTS", "NOM_STONE_GATE");
		openingFight(owner, "NOM_MINE_GATEKEEPER");
		openingMove(owner, "CITY_OUTSKIRTS", "GNOME_CITY", "CITY_SHOP");
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(gatekeeper, QuestOperation.DELIVER, "N008")).instance().status());
		var stoneDoor = acceptOpening(owner, "N009");
		var doorOre = new LifeShopRequest(UUID.randomUUID().toString(), "LIFE_ORE_0", 4);
		long beforeDoorOre = queries.find(owner).gold();
		lifeShop.buy(owner, doorOre);
		lifeShop.buy(owner, doorOre);
		assertEquals(beforeDoorOre - 32, queries.find(owner).gold());
		openingMove(owner, "GNOME_CITY", "CITY_OUTSKIRTS", "NOM_STONE_GATE");
		long beforeDoorXp = queries.find(owner).experience();
		var doorDelivery = request(stoneDoor, QuestOperation.DELIVER, "N009");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, doorDelivery).instance().status());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("NOM_STONE_KEY")));
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("LIFE_ORE_0")));
		runtime.command(owner, doorDelivery);
		assertEquals(beforeDoorOre - 32 + 30, queries.find(owner).gold());
		assertEquals(beforeDoorXp + 500, queries.find(owner).experience());
		openingMove(owner, "NOM_INNER_SEAL");
		openingMove(owner, "NOM_STONE_GATE", "CITY_OUTSKIRTS", "GNOME_CITY", "CITY_SHOP");
		for (String investigation : List.of("N010", "N011", "N012")) {
			var investigationState = acceptOpening(owner, investigation);
			openingMove(owner, "GNOME_CITY", "CITY_PETS");
			openingBattles.recover(owner);
			openingMove(owner, "GNOME_CITY", "CITY_OUTSKIRTS", "NOM_STONE_GATE", "NOM_INNER_SEAL");
			boolean seal = investigation.equals("N010");
			if (!seal)
				openingMove(owner, "NOM_OLD_YARD");
			openingFight(owner, seal ? "NOM_SEAL_GUARD" : "NOM_YARD_PATROL");
			var recoveryRequest = new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(),
					null);
			var recovered = sceneCommands.execute(owner, investigation + "_RECOVER", recoveryRequest);
			assertEquals(recovered, sceneCommands.execute(owner, investigation + "_RECOVER", recoveryRequest));
			if (!seal)
				openingMove(owner, "NOM_INNER_SEAL");
			openingMove(owner, "NOM_STONE_GATE", "CITY_OUTSKIRTS", "GNOME_CITY", "CITY_SHOP");
			long beforeInvestigationGold = queries.find(owner).gold();
			long beforeInvestigationXp = queries.find(owner).experience();
			var investigationDelivery = request(investigationState, QuestOperation.DELIVER, investigation);
			assertEquals(QuestStatus.COMPLETED, runtime.command(owner, investigationDelivery).instance().status());
			runtime.command(owner, investigationDelivery);
			assertEquals(beforeInvestigationGold + 30, queries.find(owner).gold());
			long investigationXp = switch (investigation) {
				case "N010" -> 17000;
				case "N011" -> 650;
				default -> 30000;
			};
			assertEquals(beforeInvestigationXp + investigationXp, queries.find(owner).experience());
			String consumed = switch (investigation) {
				case "N010" -> "NOM_SEAL_FRAGMENT";
				case "N011" -> "NOM_SHIPPING_MANIFEST";
				default -> "NOM_UNSTABLE_COMPONENT";
			};
			assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals(consumed)));
		}
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("NOM_REPLACEMENT_PART"))
					.mapToInt(item -> item.quantity())
					.sum());
		for (String task : List.of("N013", "N014")) {
			var taskState = acceptOpening(owner, task);
			int fights = task.equals("N013") ? 3 : 1;
			for (int i = 0; i < fights; i++) {
				openingMove(owner, "GNOME_CITY", "CITY_PETS");
				openingBattles.recover(owner);
				openingMove(owner, "GNOME_CITY", "CITY_OUTSKIRTS", "NOM_STONE_GATE", "NOM_INNER_SEAL", "NOM_OLD_YARD",
						"NOM_TRANSPORT_ROAD");
				if (task.equals("N014"))
					openingMove(owner, "NOM_OLD_DEVICE");
				openingFight(owner, task.equals("N013") ? "NOM_TRANSPORT_CLEAR" : "NOM_DEVICE_ELITE");
				if (task.equals("N014"))
					openingMove(owner, "NOM_TRANSPORT_ROAD");
				openingMove(owner, "NOM_OLD_YARD", "NOM_INNER_SEAL", "NOM_STONE_GATE", "CITY_OUTSKIRTS", "GNOME_CITY",
						"CITY_SHOP");
			}
			assertEquals(QuestStatus.COMPLETED,
					runtime.command(owner, request(taskState, QuestOperation.DELIVER, task)).instance().status());
		}
		var shutdown = acceptOpening(owner, "N015_INSTALL");
		openingMove(owner, "GNOME_CITY", "CITY_PETS");
		openingBattles.recover(owner);
		openingMove(owner, "GNOME_CITY", "CITY_OUTSKIRTS", "NOM_STONE_GATE", "NOM_INNER_SEAL", "NOM_OLD_YARD",
				"NOM_TRANSPORT_ROAD", "NOM_OLD_DEVICE");
		var installation = request(shutdown, QuestOperation.DELIVER, "N015_INSTALL");
		runtime.command(owner, installation);
		runtime.command(owner, installation);
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("NOM_REPLACEMENT_PART")));
		runtime.command(owner, request(shutdown, QuestOperation.DELIVER, "N015_SHUTDOWN"));
		openingFight(owner, "NOM_DEVICE_VERIFICATION");
		openingMove(owner, "NOM_TRANSPORT_ROAD", "NOM_OLD_YARD", "NOM_INNER_SEAL", "NOM_STONE_GATE", "CITY_OUTSKIRTS",
				"GNOME_CITY", "CITY_SHOP");
		long beforeShutdownGold = queries.find(owner).gold();
		long beforeShutdownXp = queries.find(owner).experience();
		var shutdownDelivery = request(shutdown, QuestOperation.DELIVER, "N015");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, shutdownDelivery).instance().status());
		runtime.command(owner, shutdownDelivery);
		assertEquals(beforeShutdownGold + 30, queries.find(owner).gold());
		assertEquals(beforeShutdownXp + 76000, queries.find(owner).experience());
		acceptOpening(owner, "N016_CLEAR");
		openingMove(owner, "GNOME_CITY", "CITY_PETS");
		openingBattles.recover(owner);
		openingMove(owner, "GNOME_CITY", "CITY_OUTSKIRTS", "NOM_STONE_GATE", "NOM_INNER_SEAL", "NOM_OLD_YARD",
				"NOM_TRANSPORT_ROAD", "NOM_OLD_DEVICE");
		openingFight(owner, "NOM_UNDERGROUND_GUARD");
		assertTrue(questQueries.done(owner, "N016_CLEAR"));
		openingMove(owner, "NOM_UNDERGROUND_ENTRY");
		assertTrue(questQueries.done(owner, "N016"));
		openingMove(owner, "NOM_OLD_DEVICE", "NOM_TRANSPORT_ROAD", "NOM_OLD_YARD", "NOM_INNER_SEAL", "NOM_STONE_GATE",
				"CITY_OUTSKIRTS", "GNOME_CITY", "CITY_SHOP");
		var reinforcement = acceptOpening(owner, "N018");
		long beforeReinforcement = queries.find(owner).gold();
		var reinforcementPurchase = new LifeShopRequest(UUID.randomUUID().toString(), "LIFE_ORE_0", 8);
		lifeShop.buy(owner, reinforcementPurchase);
		lifeShop.buy(owner, reinforcementPurchase);
		assertEquals(beforeReinforcement - 64, queries.find(owner).gold());
		var reinforcementDelivery = request(reinforcement, QuestOperation.DELIVER, "N018");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, reinforcementDelivery).instance().status());
		runtime.command(owner, reinforcementDelivery);
		assertEquals(beforeReinforcement - 64 + 30, queries.find(owner).gold());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("LIFE_ORE_0")));
		var resonance = acceptOpening(owner, "N017");
		for (String encounter : List.of("NOM_RESONANCE_NORTH", "NOM_RESONANCE_SOUTH")) {
			openingMove(owner, "GNOME_CITY", "CITY_PETS");
			openingBattles.recover(owner);
			openingMove(owner, "GNOME_CITY", "CITY_OUTSKIRTS", "NOM_STONE_GATE", "NOM_INNER_SEAL", "NOM_OLD_YARD",
					"NOM_TRANSPORT_ROAD", "NOM_OLD_DEVICE", "NOM_UNDERGROUND_ENTRY", "NOM_RESONANCE_GALLERY");
			openingFight(owner, encounter);
			openingMove(owner, "NOM_UNDERGROUND_ENTRY", "NOM_OLD_DEVICE", "NOM_TRANSPORT_ROAD", "NOM_OLD_YARD",
					"NOM_INNER_SEAL", "NOM_STONE_GATE", "CITY_OUTSKIRTS", "GNOME_CITY", "CITY_SHOP");
		}
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(resonance, QuestOperation.DELIVER, "N017")).instance().status());
		var ruin = acceptOpening(owner, "N019_OUTER");
		openingMove(owner, "GNOME_CITY", "CITY_PETS");
		openingBattles.recover(owner);
		openingMove(owner, "GNOME_CITY", "CITY_OUTSKIRTS", "NOM_STONE_GATE", "NOM_INNER_SEAL", "NOM_OLD_YARD",
				"NOM_TRANSPORT_ROAD", "NOM_OLD_DEVICE", "NOM_UNDERGROUND_ENTRY", "NOM_RESONANCE_GALLERY");
		openingFight(owner, "NOM_RUIN_OUTER_GUARD");
		assertTrue(questQueries.done(owner, "N019_OUTER"));
		openingMove(owner, "NOM_RUIN_INNER_COURT");
		openingFight(owner, "NOM_RUIN_INNER_GUARD");
		openingMove(owner, "NOM_RESONANCE_GALLERY", "NOM_UNDERGROUND_ENTRY", "NOM_OLD_DEVICE", "NOM_TRANSPORT_ROAD",
				"NOM_OLD_YARD", "NOM_INNER_SEAL", "NOM_STONE_GATE", "CITY_OUTSKIRTS", "GNOME_CITY", "CITY_SHOP");
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(ruin, QuestOperation.DELIVER, "N019")).instance().status());
		var forecourt = acceptOpening(owner, "N020_CLEAR");
		openingMove(owner, "GNOME_CITY", "CITY_PETS");
		openingBattles.recover(owner);
		openingMove(owner, "GNOME_CITY", "CITY_OUTSKIRTS", "NOM_STONE_GATE", "NOM_INNER_SEAL", "NOM_OLD_YARD",
				"NOM_TRANSPORT_ROAD", "NOM_OLD_DEVICE", "NOM_UNDERGROUND_ENTRY", "NOM_RESONANCE_GALLERY",
				"NOM_RUIN_INNER_COURT", "NOM_SEAL_FORECOURT");
		openingFight(owner, "NOM_FORECOURT_SENTINEL");
		assertTrue(questQueries.done(owner, "N020_CLEAR"));
		var activation = request(forecourt, QuestOperation.DELIVER, "N020");
		long beforeActivationGold = queries.find(owner).gold();
		long beforeActivationXp = queries.find(owner).experience();
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, activation).instance().status());
		runtime.command(owner, activation);
		assertEquals(beforeActivationGold + 30, queries.find(owner).gold());
		assertEquals(beforeActivationXp + 188000, queries.find(owner).experience());
		assertEquals(QuestStatus.ACTIVE, acceptOpening(owner, "N021").status());
		acceptOpening(owner, "N022_BRIEFING");
		acceptOpening(owner, "N023_BRIEFING");
		long sealRun = dungeonRuns.solo("NOM_SEAL", owner, UUID.randomUUID().toString());
		for (int stage = 0; stage < 3; stage++) {
			if (stage > 0)
				dungeonRuns.move(sealRun, owner, stage);
			dungeonRuns.rest(sealRun, owner);
			dungeonRuns.ready(sealRun, owner);
			long sealBattle = dungeonRuns.begin(sealRun, owner);
			var combat = dungeonCombat.view(sealRun, sealBattle, owner);
			int rounds = 0;
			while (combat.status().equals("ACTIVE")) {
				assertTrue(rounds++ < 60, "封印阶段应在60回合内结束");
				var replacements = new LinkedHashMap<String, Integer>();
				var alive = combat.units()
					.stream()
					.filter(unit -> unit.controllable())
					.map(unit -> unit.detail())
					.toList();
				var reserves = alive.stream()
					.filter(unit -> !unit.hero() && unit.position() >= 2 && unit.position() <= 3)
					.iterator();
				for (int position = 0; position < 2 && reserves.hasNext(); position++) {
					int slot = position;
					if (alive.stream().noneMatch(unit -> !unit.hero() && unit.position() == slot))
						replacements.put(reserves.next().id(), position);
				}
				var sealParty = combat.units()
					.stream()
					.filter(unit -> unit.controllable())
					.map(unit -> unit.detail())
					.filter(unit -> unit.hero() || unit.position() < 2 || replacements.containsKey(unit.id()))
					.toList();
				var injured = sealParty.stream()
					.min(Comparator.comparingDouble(unit -> (double) unit.hp() / unit.stats().health()))
					.orElseThrow();
				String target = combat.units()
					.stream()
					.map(unit -> unit.detail())
					.filter(unit -> unit.enemy() && unit.hp() > 0)
					.findFirst()
					.orElseThrow()
					.id();
				String notice = combat.telegraph() == null ? "" : combat.telegraph();
				boolean defend = notice.contains("石锤") || notice.contains("过载") || notice.contains("凝聚两道裂隙刃")
						|| notice.contains("定向脉冲");
				var orders = sealParty.stream().map(unit -> {
					boolean unitDefends = defend
							&& (unit.hero() || notice.contains("凝聚两道裂隙刃") || notice.contains("定向脉冲"));
					String action = unitDefends ? "DEFEND" : "ATTACK";
					String aim = unitDefends ? unit.id() : target;
					if (unit.hero() && injured.hp() < injured.stats().health() * 0.45
							&& unit.actions()
								.stream()
								.anyMatch(value -> value.id().equals("HEALTH_POTION") && value.available())) {
						action = "HEALTH_POTION";
						aim = injured.id();
					}
					else if (!unitDefends && injured.hp() < injured.stats().health() * 0.65
							&& unit.actions()
								.stream()
								.anyMatch(value -> value.id().equals("治疗术") && value.available())) {
						action = "治疗术";
						aim = injured.id();
					}
					else if (!unitDefends) {
						action = unit.actions()
							.stream()
							.filter(value -> value.available() && value.target().equals("ENEMY") && value.mana() > 0)
							.map(value -> value.id())
							.findFirst()
							.orElse("ATTACK");
					}
					return new BattleStateOrder(unit.id(), action, aim);
				}).toList();
				DungeonTestCommands.submit(dungeonRuns, sealRun, owner, sealBattle, combat.round(),
						new DungeonRoundStateSubmission(orders, replacements));
				combat = dungeonCombat.view(sealRun, sealBattle, owner);
			}
			assertEquals("WON", combat.status());
			assertTrue(questQueries.done(owner, "N0" + (21 + stage)));
		}
		assertNull(queries.adventure(owner).dungeon());
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
		var recovery = new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		assertThrows(RuntimeException.class, () -> sceneCommands.execute(owner, "O010_RECOVER", recovery));
		openingMove(owner, "FOREST_EDGE");
		var crate = acceptOpening(owner, "O010");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(crate, QuestOperation.DELIVER, "O010")));
		openingMove(owner, "CAVE_ENTRANCE");
		var crateRequest = new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null);
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
