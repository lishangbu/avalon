package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.battle.application.command.TrainingCommandService;
import io.github.lishangbu.avalon.battle.application.command.DungeonRunCommandService;
import io.github.lishangbu.avalon.battle.application.query.DungeonCombatQueryService;
import io.github.lishangbu.avalon.battle.application.query.DungeonRunQueryService;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundStateSubmission;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.command.SceneActionCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import io.github.lishangbu.avalon.world.application.model.SceneActionRequest;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 新号仅以正式任务、自然概率捕获和战斗推进；不写数据库、奖励或经验，种子由测试入口提供。 */
class StoryOpeningJourney {

	@Autowired
	io.github.lishangbu.avalon.content.world.WorldCatalog world;

	@Autowired
	io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService facts;

	@Autowired
	io.github.lishangbu.avalon.world.application.command.LifeSkillCommandService lifeSkills;

	@Autowired
	io.github.lishangbu.avalon.world.application.query.LifeSkillQueryService lifeSkillQueries;

	@Autowired
	io.github.lishangbu.avalon.world.application.command.LifeShopService lifeShop;

	@Autowired
	io.github.lishangbu.avalon.world.application.command.GatheringCommandService gathering;

	@Autowired
	io.github.lishangbu.avalon.world.application.command.LifeCraftingService crafting;

	@Autowired
	io.github.lishangbu.avalon.content.life.LifeProductionCatalog production;

	@Autowired
	io.github.lishangbu.avalon.world.quest.QuestRuntimeService runtime;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService queries;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService pets;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	io.github.lishangbu.avalon.content.item.ItemCatalog itemCatalog;

	@Autowired
	io.github.lishangbu.avalon.inventory.application.command.EquipmentCommerceService equipmentCommerce;

	@Autowired
	io.github.lishangbu.avalon.inventory.application.command.DungeonChestCommandService chestCommands;

	@Autowired
	io.github.lishangbu.avalon.inventory.application.query.DungeonChestQueryService chestQueries;

	@Autowired
	QuestAcceptCommandService acceptance;

	@Autowired
	QuestCommandService quests;

	@Autowired
	QuestQueryService questQueries;

	@Autowired
	BattleCommandService battles;

	@Autowired
	TrainingCommandService training;

	@Autowired
	DungeonRunCommandService dungeonRuns;

	@Autowired
	DungeonCombatQueryService dungeonCombat;

	@Autowired
	DungeonRunQueryService dungeonQueries;

	@Autowired
	SceneActionCommandService scenes;

	@Autowired
	io.github.lishangbu.avalon.market.application.command.MarketListingCommandService listings;

	@Autowired
	io.github.lishangbu.avalon.market.application.command.MarketPurchaseCommandService purchases;

	@Autowired
	io.github.lishangbu.avalon.market.application.command.MarketAccess marketAccess;

	void complete(long owner) {
		move(owner, "STONE_PATH", "SQUARE");
		accept(owner, "O001");
		move(owner, "PET_CENTER");
		deliver(owner, "O001");
		claim(owner, "O002_RECEIVE");
		creatures.position(owner, Long.parseLong(pets.list(owner).getFirst().id()), 0);
		deliver(owner, "O002");
		move(owner, "SQUARE", "WEAPON_SHOP");
		claim(owner, "O003_RECEIVE");
		equip(owner);
		deliver(owner, "O003");
		move(owner, "SQUARE", "PET_CENTER");
		battles.recover(owner);
		accept(owner, "O004");
		riverside(owner);
		deliver(owner, "O004");
		accept(owner, "O005");
		riverside(owner);
		riverside(owner);
		deliver(owner, "O005");
		move(owner, "SQUARE", "STONE_PATH", "RIVERSIDE", "FOREST_EDGE");
		accept(owner, "O006");
		for (int i = 0; i < 4; i++) {
			fight(owner, "OPENING_FOREST_CLEAR");
			if (i < 3) {
				move(owner, "RIVERSIDE", "STONE_PATH", "SQUARE", "PET_CENTER");
				battles.recover(owner);
				move(owner, "SQUARE", "STONE_PATH", "RIVERSIDE", "FOREST_EDGE");
			}
		}
		deliver(owner, "O006");
		move(owner, "RIVERSIDE", "STONE_PATH", "SQUARE", "WEAPON_SHOP");
		claim(owner, "O007");
		move(owner, "SQUARE", "PET_CENTER");
		accept(owner, "O008");
		long captured = OpeningWildTraining.capture(owner, characters, queries, battles, pets);
		creatures.position(owner, captured, 1);
		deliver(owner, "O008");
		battles.recover(owner);
		move(owner, "SQUARE", "STONE_PATH", "RIVERSIDE", "FOREST_EDGE", "CAVE_ENTRANCE");
		accept(owner, "O009");
		fight(owner, "OPENING_CAVE_BOSS");
		deliver(owner, "O009");
		equip(owner);
		move(owner, "FOREST_EDGE");
		accept(owner, "O010");
		move(owner, "CAVE_ENTRANCE");
		scenes.execute(owner, "O010_RECOVER",
				new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null));
		move(owner, "FOREST_EDGE");
		deliver(owner, "O010");
		accept(owner, "O011");
		move(owner, "PLAIN_SIGN");
		fight(owner, "OPENING_PLAINS_PATROL");
		move(owner, "STATION_OUTSKIRTS");
		deliver(owner, "O011");
		battles.recover(owner);
		move(owner, "PLAIN_SIGN");
		accept(owner, "O012");
		move(owner, "STATION_OUTSKIRTS");
		for (int i = 0; i < 4; i++) {
			move(owner, "PLAIN_SIGN");
			fight(owner, "OPENING_PLAINS_PATROL");
			move(owner, "STATION_OUTSKIRTS");
			battles.recover(owner);
		}
		deliver(owner, "O012");
		equip(owner);
		move(owner, "ACADEMY");
		accept(owner, "O013");
		var beforePets = pets.list(owner);
		var beforeItems = items.list(owner);
		var borrowed = List.of("T_CAT", "T_STONE", "T_SPROUT", "T_GALE");
		String key = UUID.randomUUID().toString();
		var practice = training.startAcademy(owner, "O013", borrowed, key);
		var won = BattleTestCommands.turn(battles, owner, Long.parseLong(practice.id()), practice.round(),
				List.of(new BattleStateOrder("trainee", "DEFEND", "trainee"),
						new BattleStateOrder("T_CAT", "SWAP", "T_SPROUT"),
						new BattleStateOrder("T_STONE", "DEFEND", "T_STONE")),
				false);
		assertEquals("WON", won.status());
		assertEquals(beforePets, pets.list(owner));
		assertEquals(beforeItems, items.list(owner));
		assertEquals(won.id(), training.startAcademy(owner, "O013", borrowed, key).id());
		deliver(owner, "O013");
		accept(owner, "O014");
		move(owner, "NOM_GATE_APPROACH", "CITY_OUTSKIRTS");
		fight(owner, "OPENING_MINE_ELITE");
		deliver(owner, "O014");
		accept(owner, "O015");
		for (int i = 0; i < 3; i++) {
			move(owner, "GNOME_CITY", "CITY_PETS");
			battles.recover(owner);
			move(owner, "GNOME_CITY", "CITY_OUTSKIRTS");
			fight(owner, "OPENING_CITY_CLEAR");
			if (i < 2)
				move(owner, "GNOME_CITY", "CITY_OUTSKIRTS");
		}
		move(owner, "GNOME_CITY");
		deliver(owner, "O015");
		for (int n = 1; n <= 15; n++)
			assertTrue(questQueries.done(owner, "O%03d".formatted(n)));
		assertTrue(queries.find(owner).level() >= 8);
	}

	void completeMineApproach(long owner) {
		move(owner, "CITY_PETS");
		battles.recover(owner);
		move(owner, "GNOME_CITY", "CITY_SHOP");
		accept(owner, "N001");
		mineTrip(owner);
		deliver(owner, "N001");
		accept(owner, "N002");
		accept(owner, "N003");
		for (int i = 0; i < 6; i++)
			mineTrip(owner);
		deliver(owner, "N002");
		gatherOre(owner);
		deliver(owner, "N003");
		accept(owner, "N004_FIGHT");
		var instance = runtime.steps(owner).get("N004_FIGHT").instance();
		for (String option : List.of("REPAIR", "FIGHT")) {
			runtime.command(owner,
					new io.github.lishangbu.avalon.world.quest.model.QuestInstanceRequest(UUID.randomUUID().toString(),
							instance.id(), instance.round(),
							io.github.lishangbu.avalon.world.quest.model.QuestOperation.CHOOSE, null, "NOM_MINE_ROUTE",
							option));
		}
		move(owner, "GNOME_CITY", "CITY_OUTSKIRTS");
		fight(owner, "NOM_OLD_ROAD_GUARD");
		move(owner, "GNOME_CITY", "CITY_SHOP");
		deliver(owner, "N004_FIGHT");
		assertTrue(questQueries.done(owner, "N004"));
		accept(owner, "N005");
	}

	void completeAbandonedPost(long owner) {
		for (int i = 0; i < 2; i++)
			recoveredFight(owner, "NOM_MINE_PASSAGE", "NOM_TUNNEL_PATROL");
		walk(owner, "NOM_ABANDONED_POST");
		deliver(owner, "N005");
		walk(owner, "CITY_SHOP");
		accept(owner, "N006");
		assertTrue(queries.find(owner).level() >= 10, "前期任务奖励与必需战斗应覆盖哨所等级门槛，无需额外刷怪");
		recoveredFight(owner, "NOM_ABANDONED_POST", "NOM_POST_GUARD");
		assertTrue(scenes
			.execute(owner, "N006_RECOVER",
					new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null))
			.completed());
		walk(owner, "CITY_SHOP");
		deliver(owner, "N006");
		accept(owner, "N007");
		for (int attempt = 0; quantity(owner, "NOM_GUARD_FRAGMENT") < 3 && attempt < 20; attempt++)
			recoveredFight(owner, "NOM_ABANDONED_POST", "NOM_POST_GUARD");
		assertTrue(quantity(owner, "NOM_GUARD_FRAGMENT") >= 3);
		walk(owner, "CITY_SHOP");
		int fragments = quantity(owner, "NOM_GUARD_FRAGMENT");
		deliver(owner, "N007");
		assertEquals(fragments - 3, quantity(owner, "NOM_GUARD_FRAGMENT"));
		for (int number = 1; number <= 7; number++)
			assertTrue(questQueries.done(owner, "N%03d".formatted(number)));
	}

	private int quantity(long owner, String code) {
		return items.list(owner).stream().filter(i -> i.code().equals(code)).mapToInt(i -> i.quantity()).sum();
	}

	void completeInnerMine(long owner) {
		walk(owner, "CITY_SHOP");
		accept(owner, "N008");
		recoveredFight(owner, "NOM_STONE_GATE", "NOM_MINE_GATEKEEPER");
		walk(owner, "CITY_SHOP");
		deliver(owner, "N008");
		accept(owner, "N009");
		gatherOre(owner, 4);
		int ore = quantity(owner, "LIFE_ORE_0");
		walk(owner, "NOM_STONE_GATE");
		deliver(owner, "N009");
		assertEquals(ore - 4, quantity(owner, "LIFE_ORE_0"));
		assertEquals(0, quantity(owner, "NOM_STONE_KEY"));
		for (String step : List.of("N010", "N011", "N012")) {
			if (!step.equals("N010"))
				assertTrue(queries.find(owner).level() >= 11, "主线奖励应满足旧堆场的11级门槛");
			walk(owner, "CITY_SHOP");
			accept(owner, step);
			recoveredFight(owner, step.equals("N010") ? "NOM_INNER_SEAL" : "NOM_OLD_YARD",
					step.equals("N010") ? "NOM_SEAL_GUARD" : "NOM_YARD_PATROL");
			assertTrue(
					scenes
						.execute(owner, step + "_RECOVER",
								new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(),
										null))
						.completed());
			walk(owner, "CITY_SHOP");
			deliver(owner, step);
		}
		accept(owner, "N013");
		assertTrue(queries.find(owner).level() >= 12, "回收装置后应满足运输旧道的12级门槛");
		for (int i = 0; i < 3; i++)
			recoveredFight(owner, "NOM_TRANSPORT_ROAD", "NOM_TRANSPORT_CLEAR");
		walk(owner, "CITY_SHOP");
		deliver(owner, "N013");
		accept(owner, "N014");
		assertTrue(queries.find(owner).level() >= 14, "清路奖励应满足旧装置平台的14级门槛");
		recoveredFight(owner, "NOM_OLD_DEVICE", "NOM_DEVICE_ELITE");
		walk(owner, "CITY_SHOP");
		deliver(owner, "N014");
		for (int number = 1; number <= 14; number++)
			assertTrue(questQueries.done(owner, "N%03d".formatted(number)));
	}

	private void recoveredFight(long owner, String location, String encounter) {
		String recovery = location.startsWith("UNDINE_")
				|| location.startsWith("U_")
						? "UNDINE_CITY"
						: location.startsWith("SYLPH_") || location.startsWith("S_") ? "SYLPH_CITY"
								: location.startsWith("SALAMANDER_") || location.startsWith("F_")
										|| location.startsWith("J_") || location.startsWith("E_") ? "SALAMANDER_CITY"
												: "CITY_PETS";
		walk(owner, recovery);
		battles.recover(owner);
		equip(owner);
		walk(owner, location);
		fight(owner, encounter);
	}

	/** 地下遗址沿真实道路推进，修补材料与共振处理分别准备后汇合。 */
	void completeUndergroundApproach(long owner) {
		walk(owner, "CITY_SHOP");
		accept(owner, "N015_INSTALL");
		walk(owner, "NOM_OLD_DEVICE");
		int parts = quantity(owner, "NOM_REPLACEMENT_PART");
		deliver(owner, "N015_INSTALL");
		assertEquals(parts - 1, quantity(owner, "NOM_REPLACEMENT_PART"));
		deliver(owner, "N015_SHUTDOWN");
		recoveredFight(owner, "NOM_OLD_DEVICE", "NOM_DEVICE_VERIFICATION");
		walk(owner, "CITY_SHOP");
		deliver(owner, "N015");
		walk(owner, "CITY_SHOP");
		accept(owner, "N016_CLEAR");
		recoveredFight(owner, "NOM_OLD_DEVICE", "NOM_UNDERGROUND_GUARD");
		deliver(owner, "N016_CLEAR");
		walk(owner, "NOM_UNDERGROUND_ENTRY");
		deliver(owner, "N016");
		walk(owner, "CITY_SHOP");
		accept(owner, "N017");
		accept(owner, "N018");
		gatherOre(owner, 8);
		int ore = quantity(owner, "LIFE_ORE_0");
		deliver(owner, "N018");
		assertEquals(ore - 8, quantity(owner, "LIFE_ORE_0"));
		assertFalse(questQueries.done(owner, "N017"), "修补物资允许先于共振处理完成");
		for (String encounter : List.of("NOM_RESONANCE_NORTH", "NOM_RESONANCE_SOUTH"))
			recoveredFight(owner, "NOM_RESONANCE_GALLERY", encounter);
		walk(owner, "CITY_SHOP");
		deliver(owner, "N017");
		accept(owner, "N019_OUTER");
		recoveredFight(owner, "NOM_RESONANCE_GALLERY", "NOM_RUIN_OUTER_GUARD");
		deliver(owner, "N019_OUTER");
		recoveredFight(owner, "NOM_RUIN_INNER_COURT", "NOM_RUIN_INNER_GUARD");
		walk(owner, "CITY_SHOP");
		deliver(owner, "N019");
		accept(owner, "N020_CLEAR");
		recoveredFight(owner, "NOM_SEAL_FORECOURT", "NOM_FORECOURT_SENTINEL");
		deliver(owner, "N020_CLEAR");
		deliver(owner, "N020");
		for (int number = 15; number <= 20; number++)
			assertTrue(questQueries.done(owner, "N%03d".formatted(number)));
	}

	void completeSealDungeon(long owner) {
		walk(owner, "CITY_PETS");
		battles.recover(owner);
		equip(owner);
		walk(owner, "NOM_SEAL_FORECOURT");
		accept(owner, "N021");
		accept(owner, "N022_BRIEFING");
		accept(owner, "N023_BRIEFING");
		completeDungeon(owner, "NOM_SEAL", null);
		walk(owner, "NOM_SEAL_FORECOURT");
		accept(owner, "N024_CLEAR");
		recoveredFight(owner, "NOM_SEAL_FORECOURT", "NOM_RETURN_GUARD");
		deliver(owner, "N024_CLEAR");
		assertTrue(scenes
			.execute(owner, "N024_RECORD",
					new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null))
			.completed());
		assertTrue(scenes
			.execute(owner, "N024_SHARD",
					new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null))
			.completed());
		walk(owner, "CITY_SHOP");
		deliver(owner, "N024");
		assertTrue(questQueries.done(owner, "N021"));
		assertTrue(questQueries.done(owner, "N022"));
		assertTrue(questQueries.done(owner, "N023"));
		assertTrue(questQueries.done(owner, "N024"));
		accept(owner, "N025");
		recoveredFight(owner, "NOM_SEAL_FORECOURT", "NOM_COORDINATE_TEST");
		walk(owner, "CITY_SHOP");
		deliver(owner, "N025");
		assertEquals(0, quantity(owner, "NOM_COORDINATE_PART"));
		assertTrue(questQueries.done(owner, "N025"));
	}

	private void completeDungeon(long owner, String dungeon, String prefix) {
		completeDungeon(owner, dungeon, prefix, 20, 3);
	}

	private void completeDungeon(long owner, String dungeon, String prefix, int offset, int stages) {
		long run = dungeonRuns.solo(dungeon, owner, UUID.randomUUID().toString());
		for (int stage = 1; stage <= stages; stage++) {
			if (prefix != null)
				accept(owner, "%s%03d".formatted(prefix, offset + stage));
			dungeonRuns.rest(run, owner);
			dungeonRuns.ready(run, owner);
			long battle = dungeonRuns.begin(run, owner);
			for (int turn = 0; turn < 80; turn++) {
				var view = dungeonCombat.view(run, battle, owner);
				if (turn == 0 && Boolean.getBoolean("avalon.test.trace"))
					System.out.printf("NATURAL_DUNGEON dungeon=%s stage=%d supplies=%s%n", dungeon, stage,
							view.supplies());
				if (!view.status().equals("ACTIVE"))
					break;
				var replacements = replacements(view.units().stream().map(u -> u.detail()).toList());
				var active = view.units()
					.stream()
					.filter(u -> u.controllable() && !u.detail().enemy() && u.detail().hp() > 0
							&& (u.detail().hero() || u.detail().position() < 2
									|| replacements.containsKey(u.detail().id())))
					.toList();
				var enemies = view.units().stream().map(u -> u.detail()).filter(u -> u.enemy() && u.hp() > 0).toList();
				var target = enemies.stream()
					.filter(e -> e.id().equals("dungeon-boss"))
					.findFirst()
					.orElse(enemies.isEmpty() ? null : enemies.getFirst());
				var injured = active.stream()
					.map(u -> u.detail())
					.min(Comparator.comparingDouble(u -> (double) u.hp() / Math.max(1, u.stats().health())))
					.orElseThrow();
				boolean warning = view.telegraph() != null
						&& (view.telegraph().contains("下一") || view.telegraph().contains("连续")
								|| view.telegraph().contains("两次") || view.telegraph().contains("锁定人物"));
				var hero = active.stream().map(u -> u.detail()).filter(u -> u.hero()).findFirst().orElse(null);
				String potion = hero == null ? null
						: hero.actions()
							.stream()
							.filter(a -> a.id().contains("HEALTH_POTION") && a.available())
							.map(a -> a.id())
							.max(String::compareTo)
							.orElse(null);
				boolean heroGuard = warning && hero != null && view.telegraph().contains(hero.name() + "。");
				boolean usePotion = !heroGuard && potion != null && injured.hp() < injured.stats().health() * 0.6;
				String healer = usePotion || injured.hp() >= injured.stats().health() * 0.7 ? null
						: active.stream()
							.map(u -> u.detail())
							.filter(u -> !warning || !view.telegraph().contains(u.name() + "。"))
							.filter(u -> u.actions().stream().anyMatch(a -> a.id().equals("微疗") && a.available()))
							.map(u -> u.id())
							.findFirst()
							.orElse(null);
				var orders = active.stream().map(u -> {
					var unit = u.detail();
					boolean guard = warning && view.telegraph().contains(unit.name() + "。");
					String action = guard ? "DEFEND" : "ATTACK";
					String aim = guard ? unit.id() : target.id();
					if (unit.id().equals(healer)) {
						action = "微疗";
						aim = injured.id();
					}
					else if (!guard && healer == null && unit.hero() && injured.hp() < injured.stats().health() * 0.7
							&& unit.actions().stream().anyMatch(a -> a.id().equals("治疗术") && a.available())) {
						action = "治疗术";
						aim = injured.id();
					}
					else if (unit.hero() && usePotion) {
						action = potion;
						aim = injured.id();
					}
					else if (!guard) {
						action = unit.actions()
							.stream()
							.filter(a -> a.available() && a.mana() > 0 && a.target().equals("ENEMY"))
							.map(a -> a.id())
							.findFirst()
							.orElse("ATTACK");
					}
					return new BattleStateOrder(unit.id(), action, aim);
				}).toList();
				dungeonRuns.submit(run, owner, battle, UUID.randomUUID(), view.revision(), view.round(),
						new DungeonRoundStateSubmission(orders, replacements));
			}
			var result = dungeonCombat.view(run, battle, owner);
			assertEquals("WON", result.status(), dungeon + " stage " + stage + ": " + result.log());
			assertEquals(stage, dungeonQueries.restore(run, owner).getCleared());
		}
		assertNull(queries.adventure(owner).dungeon());
		for (var chest : chestQueries.list(owner)) {
			chestCommands.claim(owner, Long.parseLong(chest.id()));
			var after = items.list(owner);
			chestCommands.claim(owner, Long.parseLong(chest.id()));
			assertEquals(after, items.list(owner), "副本宝箱重放不能重复发放装备与材料");
		}
	}

	/** Both city approaches are available after N025, in either order. */
	void completeCityApproach(long owner, String prefix) {
		assertTrue(List.of("U", "S").contains(prefix));
		String city = prefix.equals("U") ? "UNDINE_CITY" : "SYLPH_CITY";
		String field = prefix.equals("U") ? "UNDINE_SHALLOWS" : "SYLPH_RIDGE_ROAD";
		String entry = prefix.equals("U") ? "UNDINE_ANCIENT_INLET" : "SYLPH_OLD_POST";
		String part = prefix.equals("U") ? "UNDINE_SEAL_FRAGMENT" : "SYLPH_ROAD_BRACE";
		walk(owner, city);
		accept(owner, prefix + "001");
		recoveredFight(owner, field, prefix + "_APPROACH_PATROL");
		walk(owner, city);
		deliver(owner, prefix + "001");
		accept(owner, prefix + "002");
		accept(owner, prefix + "003");
		for (int fight = 0; fight < 4; fight++)
			recoveredFight(owner, field, prefix + "_APPROACH_PATROL");
		walk(owner, city);
		int parts = quantity(owner, part);
		deliver(owner, prefix + "003");
		assertEquals(parts - 4, quantity(owner, part));
		assertFalse(questQueries.done(owner, prefix + "002"));
		deliver(owner, prefix + "002");
		accept(owner, prefix + "004");
		recoveredFight(owner, field, prefix + "_APPROACH_BOSS");
		walk(owner, city);
		deliver(owner, prefix + "004");
		accept(owner, prefix + "005");
		walk(owner, entry);
		assertTrue(scenes
			.execute(owner, prefix + "005_OPEN",
					new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null))
			.completed());
		walk(owner, city);
		deliver(owner, prefix + "005");
		for (int number = 1; number <= 5; number++)
			assertTrue(questQueries.done(owner, "%s%03d".formatted(prefix, number)));
	}

	/** 两城可选资源链使用正常专业名额、自动采集与基础加工，不预置材料或专业经验。 */
	void completeSupplySide(long owner, String prefix) {
		boolean water = prefix.equals("U");
		String city = water ? "UNDINE_CITY" : "SYLPH_CITY";
		String field = water ? "UNDINE_SHALLOWS" : "SYLPH_RIDGE_ROAD";
		String resource = water ? "LIFE_HERB_0" : "LIFE_ORE_0";
		String tool = water ? "TOOL_WOOD_PINCERS" : "TOOL_WOOD_PICKAXE";
		String product = water ? "LIFE_REAGENT_0" : "LIFE_PART_0";
		String skill = water ? "HERBALISM" : "MINING";
		var profession = io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill
			.valueOf(water ? "ALCHEMY" : "ARTISAN");
		if (water && lifeSkillQueries.list(owner).stream().anyMatch(s -> s.code().equals("LOGGING") && s.learned()))
			lifeSkills.change(owner,
					new io.github.lishangbu.avalon.world.application.model.LifeSkillRequest(
							UUID.randomUUID().toString(),
							io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill.LOGGING, true),
					true);
		walk(owner, city);
		accept(owner, prefix + "026");
		walk(owner, field);
		walk(owner, city);
		deliver(owner, prefix + "026");
		accept(owner, prefix + "027");
		gatherMaterial(owner, quantity(owner, resource) + 1, skill, tool, field, prefix + "_SUPPLY_NODE", resource);
		walk(owner, city);
		int tools = quantity(owner, tool);
		deliver(owner, prefix + "027");
		assertEquals(tools, quantity(owner, tool));
		accept(owner, prefix + "028");
		gatherMaterial(owner, quantity(owner, resource) + 3, skill, tool, field, prefix + "_SUPPLY_NODE", resource);
		walk(owner, city);
		deliver(owner, prefix + "028");
		accept(owner, prefix + "029");
		lifeSkills.change(owner, new io.github.lishangbu.avalon.world.application.model.LifeSkillRequest(
				UUID.randomUUID().toString(), profession, false), false);
		var recipe = production.recipe(product);
		int required = 6 + recipe.ingredients().get(resource);
		gatherMaterial(owner, required, skill, tool, field, prefix + "_SUPPLY_NODE", resource);
		walk(owner, city);
		var selected = new java.util.TreeMap<String, Integer>();
		for (var ingredient : recipe.ingredients().entrySet()) {
			int remaining = ingredient.getValue();
			for (var item : items.list(owner)) {
				if (!item.code().equals(ingredient.getKey()))
					continue;
				int take = Math.min(remaining, item.quantity());
				if (take > 0)
					selected.put(item.id(), take);
				remaining -= take;
			}
			assertEquals(0, remaining, "自然制造原料不足：" + ingredient.getKey());
		}
		var craftingTool = items.list(owner)
			.stream()
			.filter(i -> i.code().equals("TOOL_WOOD_" + profession.tool))
			.findFirst()
			.orElseThrow();
		var request = new io.github.lishangbu.avalon.world.application.model.LifeCraftingRequest(
				UUID.randomUUID().toString(), recipe.id(), 1, craftingTool.id(), 1, selected);
		int before = quantity(owner, product);
		var result = crafting.craft(owner, request);
		assertEquals(1, result.completed());
		assertEquals(result, crafting.craft(owner, request));
		assertEquals(before + recipe.quantity(), quantity(owner, product));
		deliver(owner, prefix + "029");
		assertEquals(before + recipe.quantity(), quantity(owner, product));
		accept(owner, prefix + "030");
		int resources = quantity(owner, resource), products = quantity(owner, product);
		deliver(owner, prefix + "030");
		assertEquals(resources - 6, quantity(owner, resource));
		assertEquals(products - 1, quantity(owner, product));
		if (water)
			lifeSkills.change(owner, new io.github.lishangbu.avalon.world.application.model.LifeSkillRequest(
					UUID.randomUUID().toString(), profession, true), true);
	}

	/** 悬赏从清敌、战斗掉落到实物归档；同一枚印记不能抵扣两次。 */
	void completeBountySide(long owner, String prefix) {
		String city = prefix.equals("U") ? "UNDINE_CITY" : "SYLPH_CITY";
		String field = prefix.equals("U") ? "UNDINE_SHALLOWS" : "SYLPH_RIDGE_ROAD";
		trainToLevel(owner, 25, field, prefix + "_APPROACH_PATROL", 30);
		walk(owner, city);
		accept(owner, prefix + "051");
		for (int battle = 0; battle < 5; battle++)
			recoveredFight(owner, field, prefix + "_APPROACH_PATROL");
		walk(owner, city);
		deliver(owner, prefix + "051");
		accept(owner, prefix + "052");
		String material = prefix.equals("U") ? "UNDINE_SEAL_FRAGMENT" : "S_BOUNTY_MARK";
		if (prefix.equals("S"))
			recoveredFight(owner, field, "S_BOUNTY_ELITE");
		walk(owner, city);
		int before = quantity(owner, material);
		deliver(owner, prefix + "052");
		assertEquals(before - (prefix.equals("U") ? 4 : 1), quantity(owner, material));
		accept(owner, prefix + "053");
		walk(owner, field);
		walk(owner, city);
		deliver(owner, prefix + "053");
		accept(owner, prefix + "054");
		recoveredFight(owner, field, prefix + "_BOUNTY_ELITE");
		walk(owner, city);
		deliver(owner, prefix + "054");
		before = quantity(owner, prefix + "_BOUNTY_MARK");
		claim(owner, prefix + "055");
		assertEquals(before - 1, quantity(owner, prefix + "_BOUNTY_MARK"));
	}

	/** 高地旧营地的掉落、首领与完整人物护衣奖励。 */
	void completeSylphCampSide(long owner) {
		String city = "SYLPH_CITY", field = "SYLPH_OLD_POST";
		walk(owner, city);
		accept(owner, "S041");
		walk(owner, field);
		walk(owner, city);
		deliver(owner, "S041");
		accept(owner, "S042");
		for (int battle = 0; battle < 3; battle++)
			recoveredFight(owner, field, "S_CAMP_PATROL");
		walk(owner, city);
		deliver(owner, "S042");
		int fragments = quantity(owner, "S_CAMP_FRAGMENT");
		claim(owner, "S043");
		assertEquals(fragments - 6, quantity(owner, "S_CAMP_FRAGMENT"));
		accept(owner, "S044");
		recoveredFight(owner, field, "S_CAMP_KEEPER");
		walk(owner, city);
		deliver(owner, "S044");
		assertEquals(1, quantity(owner, "S_HUNTER_RELIC"));
		claim(owner, "S045");
		assertEquals(0, quantity(owner, "S_HUNTER_RELIC"));
		assertEquals(1, quantity(owner, "S_HUNTER_BODY"));
	}

	/** 遗址准备沿真实采集、交付和可替代通路推进。 */
	void completeCityPreparation(long owner, String prefix, boolean repair) {
		assertTrue(List.of("U", "S").contains(prefix));
		String city = prefix.equals("U") ? "UNDINE_CITY" : "SYLPH_CITY";
		String entry = prefix.equals("U") ? "UNDINE_ANCIENT_INLET" : "SYLPH_OLD_POST";
		walk(owner, city);
		accept(owner, prefix + "006");
		if (prefix.equals("U")) {
			walk(owner, "CITY_SHOP");
			lifeShop.buy(owner, new io.github.lishangbu.avalon.world.application.model.LifeShopRequest(
					UUID.randomUUID().toString(), "TOOL_WOOD_HAMMER", 1));
		}
		else
			walk(owner, entry);
		walk(owner, city);
		deliver(owner, prefix + "006");
		accept(owner, prefix + "007");
		gatherOre(owner, 6);
		gatherWood(owner, 6);
		walk(owner, city);
		int wood = quantity(owner, "LIFE_WOOD_0");
		int ore = quantity(owner, "LIFE_ORE_0");
		deliver(owner, prefix + "007");
		assertEquals(wood - 6, quantity(owner, "LIFE_WOOD_0"));
		assertEquals(ore - 6, quantity(owner, "LIFE_ORE_0"));
		accept(owner, prefix + "008");
		recoveredFight(owner, entry, prefix + "_RELIC_GUARD");
		walk(owner, city);
		deliver(owner, prefix + "008");
		for (int number = 9; number <= 10; number++) {
			String step = "%s%03d".formatted(prefix, number);
			accept(owner, step);
			walk(owner, entry);
			assertTrue(
					scenes
						.execute(owner, step + (number == 9 ? "_RECOVER" : "_IMPRINT"),
								new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(),
										null))
						.completed());
			walk(owner, city);
			deliver(owner, step);
		}
		String route = prefix + "011_" + (repair ? "REPAIR" : "FIGHT");
		accept(owner, route);
		var instance = runtime.steps(owner).get(route).instance();
		runtime.command(owner,
				new io.github.lishangbu.avalon.world.quest.model.QuestInstanceRequest(UUID.randomUUID().toString(),
						instance.id(), instance.round(),
						io.github.lishangbu.avalon.world.quest.model.QuestOperation.CHOOSE, null,
						prefix + "_DEEP_ROUTE", repair ? "REPAIR" : "FIGHT"));
		if (repair)
			gatherWood(owner, 8);
		else
			recoveredFight(owner, entry, prefix + "_ROUTE_GUARD");
		walk(owner, city);
		deliver(owner, route);
		assertTrue(questQueries.done(owner, prefix + "011"));
		accept(owner, prefix + "012");
		walk(owner, entry);
		assertTrue(scenes
			.execute(owner, prefix + "012_ACTIVATE",
					new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null))
			.completed());
		walk(owner, city);
		deliver(owner, prefix + "012");
		for (int number = 6; number <= 12; number++)
			assertTrue(questQueries.done(owner, "%s%03d".formatted(prefix, number)));
	}

	/** 深入两城遗址，保留真实双怪、材料消耗与精英战斗。 */
	void completeCityForecourt(long owner, String prefix) {
		assertTrue(List.of("U", "S").contains(prefix));
		String city = prefix.equals("U") ? "UNDINE_CITY" : "SYLPH_CITY";
		String deep = prefix + "_DEEP_APPROACH";
		String forecourt = prefix + "_SEAL_FORECOURT";
		String field = prefix.equals("U") ? "UNDINE_SHALLOWS" : "SYLPH_RIDGE_ROAD";
		trainToLevel(owner, 24, field, prefix + "_APPROACH_PATROL", 50);
		walk(owner, city);
		accept(owner, prefix + "013");
		for (int battle = 0; battle < 4; battle++)
			recoveredFight(owner, deep, prefix + "_DEEP_PATROL");
		walk(owner, city);
		deliver(owner, prefix + "013");
		accept(owner, prefix + "014");
		recoveredFight(owner, deep, prefix + "_RESONANT_BEAST");
		walk(owner, city);
		deliver(owner, prefix + "014");
		accept(owner, prefix + "015");
		walk(owner, deep);
		operate(owner, prefix + "015_SHUTDOWN");
		walk(owner, city);
		deliver(owner, prefix + "015");
		accept(owner, prefix + "016");
		walk(owner, forecourt);
		walk(owner, city);
		deliver(owner, prefix + "016");
		accept(owner, prefix + "017");
		gatherOre(owner, 8);
		gatherWood(owner, 4);
		walk(owner, city);
		int ore = quantity(owner, "LIFE_ORE_0");
		int wood = quantity(owner, "LIFE_WOOD_0");
		deliver(owner, prefix + "017");
		assertEquals(ore - 8, quantity(owner, "LIFE_ORE_0"));
		assertEquals(wood - 4, quantity(owner, "LIFE_WOOD_0"));
		accept(owner, prefix + "018");
		recoveredFight(owner, forecourt, prefix + "_SEAL_SPAWN");
		walk(owner, city);
		deliver(owner, prefix + "018");
		accept(owner, prefix + "019");
		recoveredFight(owner, forecourt, prefix + "_SEAL_ELITE");
		operate(owner, prefix + "019_UNLOCK");
		walk(owner, city);
		deliver(owner, prefix + "019");
		for (int number = 13; number <= 19; number++)
			assertTrue(questQueries.done(owner, "%s%03d".formatted(prefix, number)));
	}

	void completeCitySeal(long owner, String prefix) {
		assertTrue(List.of("U", "S").contains(prefix));
		String city = prefix.equals("U") ? "UNDINE_CITY" : "SYLPH_CITY";
		String forecourt = prefix + "_SEAL_FORECOURT";
		trainToLevel(owner, 28, prefix + "_DEEP_APPROACH", prefix + "_DEEP_PATROL", 50);
		walk(owner, city);
		accept(owner, prefix + "020");
		walk(owner, forecourt);
		walk(owner, city);
		deliver(owner, prefix + "020");
		walk(owner, city);
		battles.recover(owner);
		equip(owner);
		walk(owner, forecourt);
		completeDungeon(owner, prefix + "_SEAL", prefix);
		walk(owner, city);
		accept(owner, prefix + "024");
		walk(owner, forecourt);
		operate(owner, prefix + "024_RECOVER");
		walk(owner, city);
		deliver(owner, prefix + "024");
		int imprint = quantity(owner, prefix + "_NODE_RECORD");
		claim(owner, prefix + "025");
		assertEquals(imprint, quantity(owner, prefix + "_NODE_RECORD"));
		for (int number = 20; number <= 25; number++)
			assertTrue(questQueries.done(owner, "%s%03d".formatted(prefix, number)));
	}

	/** 区域捕捉支线取得水系伙伴，捕获与球消耗均使用正式概率。 */
	void prepareWaterPartner(long owner) {
		walk(owner, "UNDINE_CITY");
		accept(owner, "U031");
		captureEncounter(owner, "UNDINE_CITY", "UNDINE_SHALLOWS", "U_CAPTURE_31", "STRONG_SEAL_II");
		walk(owner, "UNDINE_CITY");
		deliver(owner, "U031");
		accept(owner, "U032");
		var before = pets.list(owner).stream().map(pet -> pet.id()).toList();
		captureEncounter(owner, "UNDINE_CITY", "UNDINE_SHALLOWS", "U_CAPTURE_32", "STRONG_SEAL_II");
		var captured = pets.list(owner).stream().filter(pet -> !before.contains(pet.id())).findFirst().orElseThrow();
		walk(owner, "UNDINE_CITY");
		deliver(owner, "U032");
		trainToLevel(owner, captured.level(), "UNDINE_SHALLOWS", "U_APPROACH_PATROL", 10);
		walk(owner, "CITY_PETS");
		creatures.position(owner, Long.parseLong(captured.id()), 1);
		assertTrue(
				pets.list(owner).stream().anyMatch(pet -> pet.id().equals(captured.id()) && pet.partyPosition() == 1));
		battles.recover(owner);
	}

	/** 四城成果通向火山节点，两个停用分支均使用正式资源与战斗。 */
	void completeFire(long owner, boolean repair) {
		String city = "SALAMANDER_CITY";
		String field = "SALAMANDER_ASH_ROAD";
		String entry = "SALAMANDER_SEALED_WORKS";
		String deep = "F_CORE_APPROACH";
		String forecourt = "F_SEAL_FORECOURT";
		walk(owner, city);
		buyDeficit(owner, "LIFE_HEALTH_POTION_3", 12);
		// 制造路线可能已为银档采集完成F001；续接主线时保留已完成实例，不重复接取或领奖。
		if (!questQueries.done(owner, "F001")) {
			accept(owner, "F001");
			walk(owner, field);
			walk(owner, city);
			deliver(owner, "F001");
		}
		accept(owner, "F002");
		accept(owner, "F003");
		for (int fight = 0; fight < 5; fight++)
			recoveredFight(owner, field, "F_ASH_PATROL");
		walk(owner, city);
		int parts = quantity(owner, "F_ANCIENT_PART");
		deliver(owner, "F003");
		assertEquals(parts - 4, quantity(owner, "F_ANCIENT_PART"));
		deliver(owner, "F002");
		accept(owner, "F004");
		recoveredFight(owner, field, "F_ROAD_KEEPER");
		walk(owner, city);
		deliver(owner, "F004");
		for (int number = 5; number <= 7; number++) {
			String step = "F%03d".formatted(number);
			accept(owner, step);
			walk(owner, entry);
			operate(owner, step + switch (number) {
				case 5 -> "_OPEN";
				case 6 -> "_RECORDS";
				default -> "_BLUEPRINT";
			});
			walk(owner, city);
			deliver(owner, step);
		}
		accept(owner, "F008");
		recoveredFight(owner, entry, "F_FACILITY_GUARD");
		walk(owner, city);
		deliver(owner, "F008");
		accept(owner, "F009");
		gatherOre(owner, 10);
		gatherWood(owner, 6);
		walk(owner, city);
		int ore = quantity(owner, "LIFE_ORE_0");
		int wood = quantity(owner, "LIFE_WOOD_0");
		deliver(owner, "F009");
		assertEquals(ore - 10, quantity(owner, "LIFE_ORE_0"));
		assertEquals(wood - 6, quantity(owner, "LIFE_WOOD_0"));
		accept(owner, "F010");
		walk(owner, entry);
		operate(owner, "F010_CONNECTION");
		walk(owner, city);
		deliver(owner, "F010");
		accept(owner, "F011");
		walk(owner, entry);
		walk(owner, city);
		deliver(owner, "F011");
		String route = "F012_" + (repair ? "REPAIR" : "FIGHT");
		accept(owner, route);
		var instance = runtime.steps(owner).get(route).instance();
		runtime.command(owner,
				new io.github.lishangbu.avalon.world.quest.model.QuestInstanceRequest(UUID.randomUUID().toString(),
						instance.id(), instance.round(),
						io.github.lishangbu.avalon.world.quest.model.QuestOperation.CHOOSE, null, "F_SHUTDOWN_ROUTE",
						repair ? "REPAIR" : "FIGHT"));
		if (repair)
			gatherOre(owner, 10);
		else
			recoveredFight(owner, entry, "F_BYPASS_GUARD");
		walk(owner, city);
		deliver(owner, route);
		trainToLevel(owner, 31, field, "F_ASH_PATROL", 50);
		walk(owner, city);
		accept(owner, "F013");
		for (int fight = 0; fight < 5; fight++)
			recoveredFight(owner, deep, "F_CORE_PATROL");
		walk(owner, city);
		deliver(owner, "F013");
		accept(owner, "F014");
		recoveredFight(owner, deep, "F_OVERFLOW_SPAWN");
		operate(owner, "F014_SHUTDOWN");
		walk(owner, city);
		deliver(owner, "F014");
		accept(owner, "F015");
		walk(owner, deep);
		operate(owner, "F015_UNLOCK");
		walk(owner, city);
		deliver(owner, "F015");
		accept(owner, "F016");
		gatherMaterial(owner, quantity(owner, "LIFE_ORE_0") + 3, "MINING", "TOOL_WOOD_PICKAXE", city, "F_REPAIR_ORE",
				"LIFE_ORE_0");
		walk(owner, city);
		deliver(owner, "F016");
		trainToLevel(owner, 33, deep, "F_CORE_PATROL", 50);
		walk(owner, city);
		accept(owner, "F017");
		recoveredFight(owner, forecourt, "F_VOLCANIC_ELITE");
		walk(owner, city);
		deliver(owner, "F017");
		accept(owner, "F018");
		for (int fight = 0; fight < 2; fight++)
			recoveredFight(owner, forecourt, "F_AFFECTED_PACK");
		walk(owner, city);
		deliver(owner, "F018");
		accept(owner, "F019");
		recoveredFight(owner, forecourt, "F_UNDERGROUND_GUARD");
		operate(owner, "F019_UNLOCK");
		walk(owner, city);
		deliver(owner, "F019");
		accept(owner, "F020");
		walk(owner, forecourt);
		walk(owner, city);
		deliver(owner, "F020");
		trainToLevel(owner, 36, forecourt, "F_AFFECTED_PACK", 50);
		walk(owner, city);
		lifeShop.buy(owner, new io.github.lishangbu.avalon.world.application.model.LifeShopRequest(
				UUID.randomUUID().toString(), "LIFE_HEALTH_POTION_3", 12));
		lifeShop.buy(owner, new io.github.lishangbu.avalon.world.application.model.LifeShopRequest(
				UUID.randomUUID().toString(), "LIFE_MANA_POTION_3", 6));
		battles.recover(owner);
		equip(owner);
		walk(owner, forecourt);
		completeDungeon(owner, "F_SEAL", "F");
		walk(owner, city);
		accept(owner, "F024");
		walk(owner, forecourt);
		operate(owner, "F024_RECOVER");
		walk(owner, city);
		deliver(owner, "F024");
		assertEquals(0, quantity(owner, "F_CORE_LOCATION"));
		int dossier = quantity(owner, "F_JOINT_DOSSIER");
		claim(owner, "F025");
		assertEquals(dossier, quantity(owner, "F_JOINT_DOSSIER"));
		for (int number = 1; number <= 25; number++)
			assertTrue(questQueries.done(owner, "F%03d".formatted(number)));
	}

	private void operate(long owner, String action) {
		assertTrue(scenes
			.execute(owner, action,
					new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), null))
			.completed());
	}

	private void trainToLevel(long owner, int level, String location, String encounter, int budget) {
		int count = 0;
		long start = queries.find(owner).experience();
		while (queries.find(owner).level() < level && count < budget) {
			recoveredFight(owner, location, encounter);
			count++;
		}
		if (Boolean.getBoolean("avalon.test.trace"))
			System.out.printf("NATURAL_GROWTH encounter=%s target=%d fights=%d xp=%d->%d gold=%d%n", encounter, level,
					count, start, queries.find(owner).experience(), queries.find(owner).gold());
		assertTrue(queries.find(owner).level() >= level, "正式刷怪路线超出额外战斗预算：" + encounter);
	}

	/** 联合物资、外围清理、五阶段深渊和四城收尾均通过正式入口。 */
	void completeJoint(long owner) {
		String city = "SALAMANDER_CITY", field = "J_CORE_PERIMETER", entrance = "J_ABYSS_ENTRANCE";
		clearOutgrownEquipment(owner);
		prepareJointInventory(owner);
		walk(owner, city);
		for (int number = 1; number <= 4; number++)
			accept(owner, "J%03d".formatted(number));
		gatherOre(owner, 12);
		gatherWood(owner, 8);
		walk(owner, city);
		for (var material : java.util.Map
			.of("LIFE_HERB_0", 12, "LIFE_REAGENT_0", 2, "LIFE_PART_0", 3, "LIFE_WRAP_0", 3, "LIFE_ORE_3", 6,
					"LIFE_PART_3", 2)
			.entrySet())
			buyDeficit(owner, material.getKey(), material.getValue());
		for (int number = 1; number <= 4; number++)
			deliver(owner, "J%03d".formatted(number));
		claim(owner, "J005");
		assertEquals(0, quantity(owner, "F_JOINT_DOSSIER"));
		for (int number = 6; number <= 9; number++) {
			String step = "J%03d".formatted(number);
			accept(owner, step);
			String encounter = number == 7 ? "J_PERIMETER_ELITE"
					: number == 8 ? "J_UNSTABLE_GUARD" : "J_PERIMETER_PATROL";
			int count = number == 6 ? 5 : number == 9 ? 3 : 1;
			for (int fight = 0; fight < count; fight++)
				recoveredFight(owner, field, encounter);
			walk(owner, city);
			deliver(owner, step);
		}
		claim(owner, "J010");
		accept(owner, "J011");
		walk(owner, field);
		operate(owner, "J011_RECOVER");
		walk(owner, city);
		deliver(owner, "J011");
		accept(owner, "J012");
		buyDeficit(owner, "LIFE_PART_3", 4);
		buyDeficit(owner, "LIFE_REAGENT_3", 4);
		deliver(owner, "J012");
		accept(owner, "J013");
		recoveredFight(owner, field, "J_ENTRANCE_GUARD");
		walk(owner, city);
		deliver(owner, "J013");
		accept(owner, "J014");
		walk(owner, field);
		operate(owner, "J014_OPEN");
		walk(owner, city);
		deliver(owner, "J014");
		accept(owner, "J015");
		walk(owner, entrance);
		walk(owner, city);
		deliver(owner, "J015");
		trainToLevel(owner, 40, field, "J_PERIMETER_PATROL", 60);
		walk(owner, city);
		buyDeficit(owner, "LIFE_HEALTH_POTION_4", 20);
		buyDeficit(owner, "LIFE_MANA_POTION_4", 10);
		battles.recover(owner);
		equip(owner);
		walk(owner, entrance);
		completeDungeon(owner, "J_ABYSS", "J", 15, 5);
		accept(owner, "J021");
		operate(owner, "J021_RECORD");
		deliver(owner, "J021");
		int report = quantity(owner, "J_CORE_REPORT");
		accept(owner, "J022");
		walk(owner, city);
		deliver(owner, "J022");
		assertEquals(report, quantity(owner, "J_CORE_REPORT"));
		accept(owner, "J023");
		walk(owner, field);
		operate(owner, "J023_SEAL");
		walk(owner, city);
		deliver(owner, "J023");
		assertEquals(0, quantity(owner, "J_CORE_REPORT"));
		accept(owner, "J024");
		for (String destination : List.of("GNOME_CITY", "UNDINE_CITY", "SYLPH_CITY", city))
			walk(owner, destination);
		deliver(owner, "J024");
		claim(owner, "J025");
		assertEquals(0, quantity(owner, "J_REWARD_VOUCHER"));
		for (int number = 26; number <= 30; number++) {
			String step = "J%03d".formatted(number);
			accept(owner, step);
			walk(owner, number == 28 ? "E_CHALLENGE_ENTRANCE" : number == 29 ? "SQUARE" : "J_RESTORED_CROSSROADS");
			walk(owner, city);
			deliver(owner, step);
		}
		assertTrue(questQueries.done(owner, "J030"));
	}

	/** 主线后残留区保留真实掉落、交付和重复战斗。 */
	void completeResidual(long owner) {
		String city = "SALAMANDER_CITY", field = "E_RESIDUAL_FIELD";
		walk(owner, city);
		accept(owner, "E001");
		walk(owner, field);
		walk(owner, city);
		deliver(owner, "E001");
		accept(owner, "E002");
		for (int count = 0; count < 6; count++)
			recoveredFight(owner, field, "E_RESIDUAL_PATROL");
		walk(owner, city);
		deliver(owner, "E002");
		accept(owner, "E003");
		recoveredFight(owner, field, "E_RESIDUAL_ELITE");
		walk(owner, city);
		deliver(owner, "E003");
		int before = quantity(owner, "E_TEMPERED_FRAGMENT");
		claim(owner, "E004");
		assertEquals(before - 10, quantity(owner, "E_TEMPERED_FRAGMENT"));
		accept(owner, "E005");
		for (int count = 0; count < 3; count++)
			recoveredFight(owner, field, "E_RESIDUAL_PATROL");
		walk(owner, city);
		deliver(owner, "E005");
		trainToLevel(owner, 45, field, "E_RESIDUAL_PATROL", 80);
		clearOutgrownEquipment(owner);
		walk(owner, city);
		accept(owner, "E006");
		walk(owner, "E_CHALLENGE_ENTRANCE");
		walk(owner, city);
		deliver(owner, "E006");
		accept(owner, "E007");
		buyDeficit(owner, "LIFE_HEALTH_POTION_4", 20);
		buyDeficit(owner, "LIFE_MANA_POTION_4", 10);
		battles.recover(owner);
		equip(owner);
		walk(owner, "E_CHALLENGE_ENTRANCE");
		completeDungeon(owner, "E_SEALED_TRIAL", null, 0, 2);
		walk(owner, city);
		deliver(owner, "E007");
		accept(owner, "E008");
		walk(owner, "E_CHALLENGE_ENTRANCE");
		walk(owner, city);
		deliver(owner, "E008");
		assertEquals(1, quantity(owner, "E_FORGE_BLUEPRINT"));
		// 重复副本独立于可选制皮成长；首次通关后，当日剩余两次入场即可完成。
		assertTrue(!questQueries.done(owner, "E009"));
		accept(owner, "E010");
		for (int repeat = 0; repeat < 2; repeat++) {
			var beforeSupply = queries.find(owner);
			int supplyHealthBefore = quantity(owner, "LIFE_HEALTH_POTION_4");
			int supplyManaBefore = quantity(owner, "LIFE_MANA_POTION_4");
			buyDeficit(owner, "LIFE_HEALTH_POTION_4", 20);
			buyDeficit(owner, "LIFE_MANA_POTION_4", 10);
			long purchaseGold = beforeSupply.gold() - queries.find(owner).gold();
			battles.recover(owner);
			equip(owner);
			walk(owner, "E_CHALLENGE_ENTRANCE");
			var beforeTrial = queries.find(owner);
			int healthBefore = quantity(owner, "LIFE_HEALTH_POTION_4");
			int manaBefore = quantity(owner, "LIFE_MANA_POTION_4");
			completeDungeon(owner, "E_SEALED_TRIAL", null, 0, 2);
			var afterTrial = queries.find(owner);
			assertTrue(afterTrial.experience() > beforeTrial.experience(), "重复试炼必须继续提供成长经验");
			System.out.printf(
					"POSTGAME_TRIAL repeat=%d purchaseGold=%d experienceGain=%d goldGain=%d healthBefore=%d healthAfter=%d manaBefore=%d manaAfter=%d%n",
					repeat + 1, purchaseGold, afterTrial.experience() - beforeTrial.experience(),
					afterTrial.gold() - beforeTrial.gold(), healthBefore, quantity(owner, "LIFE_HEALTH_POTION_4"),
					manaBefore, quantity(owner, "LIFE_MANA_POTION_4"));
			walk(owner, city);
			int patrols = 0;
			while (queries.find(owner).gold() < beforeSupply.gold()
					|| quantity(owner, "LIFE_HEALTH_POTION_4") < supplyHealthBefore
					|| quantity(owner, "LIFE_MANA_POTION_4") < supplyManaBefore) {
				assertTrue(patrols++ < 80, "重复试炼补给回补超过巡猎预算");
				recoveredFight(owner, field, "E_RESIDUAL_PATROL");
				walk(owner, city);
				buyDeficit(owner, "LIFE_HEALTH_POTION_4", supplyHealthBefore);
				buyDeficit(owner, "LIFE_MANA_POTION_4", supplyManaBefore);
			}
			System.out.printf("POSTGAME_RECOVERY repeat=%d patrols=%d goldBefore=%d goldAfter=%d%n", repeat + 1,
					patrols, beforeSupply.gold(), queries.find(owner).gold());
		}
		deliver(owner, "E010");
		assertTrue(questQueries.done(owner, "E010"));
		completeRareCollection(owner);
	}

	void completeLeatherworkOpening(long owner) {
		completeLeatherworkGrowth(owner, 2);
	}

	void completeLeatherworkGrowth(long owner, int targetLevel) {
		completeBasicManufacturingGrowth(owner, targetLevel, "LEATHERWORK", "LIFE_WRAP_0", "LIFE_HIDE_0", "HUNTING",
				"TOOL_WOOD_TRAP", "PLAIN_SIGN", "HUNTING_1", "TOOL_WOOD_NEEDLE", "GNOME_LEATHERWORKS");
	}

	void completeAlchemyOpening(long owner) {
		completeBasicManufacturingGrowth(owner, 3, "ALCHEMY", "LIFE_REAGENT_0", "LIFE_HERB_0", "HERBALISM",
				"TOOL_WOOD_PINCERS", "RIVERSIDE", "HERBALISM_1", "TOOL_WOOD_ROD", "GNOME_APOTHECARY");
	}

	void completeArtisanOpening(long owner) {
		completeBasicManufacturingGrowth(owner, 3, "ARTISAN", "LIFE_PART_0", "LIFE_ORE_0", "MINING",
				"TOOL_WOOD_PICKAXE", "CAVE_ENTRANCE", "MINING_1", "TOOL_WOOD_HAMMER", "GNOME_FORGE");
	}

	void completeCookingOpening(long owner) {
		clearUnusedEquipment(owner, 0);
		walk(owner, "CITY_SHOP");
		for (String skill : List.of("MINING", "LOGGING")) {
			if (lifeSkillQueries.list(owner).stream().anyMatch(s -> s.code().equals(skill) && s.learned()))
				lifeSkills.change(owner,
						new io.github.lishangbu.avalon.world.application.model.LifeSkillRequest(
								UUID.randomUUID().toString(),
								io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill.valueOf(skill), true),
						true);
		}
		lifeSkills.change(owner,
				new io.github.lishangbu.avalon.world.application.model.LifeSkillRequest(UUID.randomUUID().toString(),
						io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill.COOKING, false),
				false);
		long goldBefore = queries.find(owner).gold();
		int crafts = 0;
		while (learnedLifeSkillLevel(owner, "COOKING") < 3) {
			assertTrue(crafts++ < 100, "烹饪起步超出制造预算");
			var recipe = production
				.recipe(learnedLifeSkillLevel(owner, "COOKING") == 1 ? "LIFE_RATION_0" : "LIFE_RATION_0_FINE");
			gatherMaterial(owner, recipe.ingredients().get("LIFE_HERB_0"), "HERBALISM", "TOOL_WOOD_PINCERS",
					"RIVERSIDE", "HERBALISM_1", "LIFE_HERB_0");
			gatherMaterial(owner, recipe.ingredients().get("LIFE_MEAT_0"), "HUNTING", "TOOL_WOOD_TRAP", "PLAIN_SIGN",
					"HUNTING_MEAT_1", "LIFE_MEAT_0");
			craftWithInventory(owner, recipe.id(), "TOOL_WOOD_POT", "GNOME_KITCHEN");
			sellManufacturedSurplus(owner, "LIFE_RATION_0", 20);
		}
		assertEquals(3, learnedLifeSkillLevel(owner, "COOKING"));
		assertEquals(java.util.Set.of("COOKING", "HERBALISM", "HUNTING"),
				lifeSkillQueries.list(owner)
					.stream()
					.filter(s -> s.learned() && s.profession())
					.map(s -> s.code())
					.collect(java.util.stream.Collectors.toSet()));
		System.out.printf("NATURAL_COOKING level=3 crafts=%d goldBefore=%d goldAfter=%d%n", crafts, goldBefore,
				queries.find(owner).gold());
	}

	void completeCopperCookingGrowth(long cook, long artisan, long leatherworker) {
		trainGatheringToCopper(cook, "HERBALISM", "TOOL_WOOD_PINCERS", "LIFE_HERB_0", "RIVERSIDE", "HERBALISM_1",
				"GNOME_GARDEN", "HERBALISM_2");
		trainGatheringToCopper(cook, "HUNTING", "TOOL_WOOD_TRAP", "LIFE_MEAT_0", "PLAIN_SIGN", "HUNTING_MEAT_1",
				"CITY_OUTSKIRTS", "HUNTING_MEAT_2");
		long goldBefore = queries.find(cook).gold();
		int crafts = 0;
		while (learnedLifeSkillLevel(cook, "COOKING") < 5) {
			assertTrue(crafts++ < 400, "铜档烹饪成长超出制造预算");
			var recipe = production
				.recipe(learnedLifeSkillLevel(cook, "COOKING") == 3 ? "LIFE_RATION_1" : "LIFE_RATION_1_FINE");
			String herbNode = learnedLifeSkillLevel(cook, "HERBALISM") >= 4 ? "HERBALISM_HERB_4" : "HERBALISM_HERB_3";
			String meatNode = learnedLifeSkillLevel(cook, "HUNTING") >= 4 ? "HUNTING_MEAT_4" : "HUNTING_MEAT_3";
			gatherMaterial(cook, recipe.ingredients().get("LIFE_HERB_1"), "HERBALISM", "TOOL_COPPER_PINCERS",
					"COPPER_WETLAND", herbNode, "LIFE_HERB_1", 0,
					() -> replenishLeatherworkerCopperTool(leatherworker, artisan, cook, "TOOL_COPPER_PINCERS"));
			gatherMaterial(cook, recipe.ingredients().get("LIFE_MEAT_1"), "HUNTING", "TOOL_COPPER_TRAP",
					"COPPER_WETLAND", meatNode, "LIFE_MEAT_1", 0,
					() -> replenishCopperTools(artisan, cook, "TOOL_COPPER_TRAP"));
			replenishCopperTools(artisan, cook, "TOOL_COPPER_POT");
			craftWithInventory(cook, recipe.id(), "TOOL_COPPER_POT", "GNOME_KITCHEN");
			sellManufacturedSurplus(cook, "LIFE_RATION_1", 20);
			if (crafts % 128 == 0)
				System.out.printf("NATURAL_COPPER_COOKING_PROGRESS crafts=%d gold=%d%n", crafts,
						queries.find(cook).gold());
		}
		assertEquals(5, learnedLifeSkillLevel(cook, "COOKING"));
		System.out.printf("NATURAL_COPPER_COOKING level=5 crafts=%d goldBefore=%d goldAfter=%d%n", crafts, goldBefore,
				queries.find(cook).gold());
	}

	void completeIronCookingSupply(long cook, long leatherworker, long alchemist, long artisan) {
		assertTrue(learnedLifeSkillLevel(cook, "COOKING") >= 5);
		trainGatheringToIron(cook, "HERBALISM", "TOOL_COPPER_PINCERS", "LIFE_HERB_1", "HERBALISM_HERB_",
				() -> replenishLeatherworkerCopperTool(leatherworker, artisan, cook, "TOOL_COPPER_PINCERS"));
		trainGatheringToIron(cook, "HUNTING", "TOOL_COPPER_TRAP", "LIFE_MEAT_1", "HUNTING_MEAT_",
				() -> replenishCopperTools(artisan, cook, "TOOL_COPPER_TRAP"));
		while (quantity(cook, "LIFE_RATION_2") < 2) {
			gatherMaterial(cook, 2, "HERBALISM", "TOOL_IRON_PINCERS", "IRON_HIGHLAND", "HERBALISM_HERB_5",
					"LIFE_HERB_2", 0,
					() -> replenishCooperativeIronTool(leatherworker, alchemist, artisan, cook, "TOOL_IRON_PINCERS"));
			gatherMaterial(cook, 1, "HUNTING", "TOOL_IRON_TRAP", "IRON_HIGHLAND", "HUNTING_MEAT_5", "LIFE_MEAT_2", 0,
					() -> replenishCooperativeIronTool(leatherworker, alchemist, artisan, cook, "TOOL_IRON_TRAP"));
			replenishCooperativeIronTool(leatherworker, alchemist, artisan, cook, "TOOL_IRON_POT");
			craftWithInventory(cook, "LIFE_RATION_2", "TOOL_IRON_POT", "GNOME_KITCHEN");
		}
		assertEquals(2, quantity(cook, "LIFE_RATION_2"));
		System.out.printf("NATURAL_IRON_COOKING_SUPPLY rations=%d gold=%d%n", quantity(cook, "LIFE_RATION_2"),
				queries.find(cook).gold());
	}

	/** 银档材料地图必须由该采集角色自己完成两城封存及工坊委托解锁。 */
	void unlockSilverGatheringRegion(long owner, long artisan) {
		// 两城准备各6、U011修复8、两城前庭各4，共28份，避免为剧情遗忘已有专业。
		int woodDeficit = Math.max(0, 28 - quantity(owner, "LIFE_WOOD_0"));
		// 工匠自己解锁时直接采够库存；跨角色才进行真实市场交易，禁止自己购买自己的挂单。
		gatherWood(artisan, owner == artisan ? 28 : woodDeficit);
		if (owner != artisan)
			for (int count = 0; count < woodDeficit; count++)
				tradeManufacturedTool(artisan, owner, "LIFE_WOOD_0");
		prepareWaterPartner(owner);
		for (String prefix : java.util.List.of("U", "S")) {
			completeCityApproach(owner, prefix);
			completeCityPreparation(owner, prefix, prefix.equals("U"));
			completeCityForecourt(owner, prefix);
			completeCitySeal(owner, prefix);
			assertTrue(questQueries.done(owner, prefix + "025"));
		}
		walk(owner, "SALAMANDER_CITY");
		accept(owner, "F001");
		walk(owner, "SALAMANDER_ASH_ROAD");
		walk(owner, "SALAMANDER_CITY");
		deliver(owner, "F001");
		assertTrue(questQueries.done(owner, "F001"));
		walk(owner, "SILVER_FROSTWOOD");
		System.out.println("NATURAL_SILVER_REGION unlocked=F001");
	}

	void manufactureAndTradeSilverTools(long leatherworker, long alchemist, long artisan, long cook) {
		assertEquals(7, learnedLifeSkillLevel(alchemist, "ALCHEMY"));
		assertEquals(7, learnedLifeSkillLevel(artisan, "ARTISAN"));
		trainGatheringToIron(artisan, "LOGGING", "TOOL_COPPER_AXE", "LIFE_WOOD_1", "LOGGING_WOOD_",
				() -> ensureOwnedCopperTool(artisan, "TOOL_COPPER_AXE"));
		gatherMaterial(artisan, 4, "LOGGING", "TOOL_IRON_AXE", "IRON_HIGHLAND", "LOGGING_WOOD_5", "LIFE_WOOD_2", 0,
				() -> replenishCooperativeIronTool(leatherworker, alchemist, artisan, artisan, "TOOL_IRON_AXE"));
		gatherMaterial(artisan, 4, "MINING", "TOOL_IRON_PICKAXE", "IRON_HIGHLAND", "MINING_ORE_5", "LIFE_ORE_2", 0,
				() -> replenishCooperativeIronTool(leatherworker, alchemist, artisan, artisan, "TOOL_IRON_PICKAXE"));
		for (int count = 0; count < 2; count++) {
			tradeManufacturedTool(leatherworker, artisan, "LIFE_WRAP_2");
			tradeManufacturedTool(alchemist, artisan, "LIFE_REAGENT_2");
			tradeManufacturedTool(cook, artisan, "LIFE_RATION_2");
		}
		for (var ingredient : production.recipe("TOOL_SILVER_NEEDLE").ingredients().entrySet())
			for (int count = 0; count < ingredient.getValue(); count++)
				tradeManufacturedTool(artisan, alchemist, ingredient.getKey());
		int needlesBefore = quantity(leatherworker, "TOOL_SILVER_NEEDLE");
		int trapsBefore = quantity(leatherworker, "TOOL_SILVER_TRAP");
		replenishCooperativeIronTool(leatherworker, alchemist, artisan, alchemist, "TOOL_IRON_ROD");
		craftWithInventory(alchemist, "TOOL_SILVER_NEEDLE", "TOOL_IRON_ROD", "GNOME_APOTHECARY");
		tradeManufacturedTool(alchemist, leatherworker, "TOOL_SILVER_NEEDLE");
		replenishCooperativeIronTool(leatherworker, alchemist, artisan, artisan, "TOOL_IRON_HAMMER");
		craftWithInventory(artisan, "TOOL_SILVER_TRAP", "TOOL_IRON_HAMMER", "GNOME_FORGE");
		tradeManufacturedTool(artisan, leatherworker, "TOOL_SILVER_TRAP");
		assertEquals(needlesBefore + 1, quantity(leatherworker, "TOOL_SILVER_NEEDLE"));
		assertEquals(trapsBefore + 1, quantity(leatherworker, "TOOL_SILVER_TRAP"));
		assertEquals(0, quantity(cook, "LIFE_RATION_2"));
		System.out.println("NATURAL_SILVER_DELIVERY needle=1 trap=1");
	}

	void completeSilverLeatherworkGrowth(long leatherworker, long alchemist, long artisan, long cook) {
		assertEquals(7, learnedLifeSkillLevel(leatherworker, "LEATHERWORK"));
		assertTrue(questQueries.done(leatherworker, "F001"));
		// 前几档训练会积压不同类型的包覆、兽皮和稀有物，进入银档前正常回收而非扩容背包。
		prepareManufacturingInventory(leatherworker, java.util.Map.of());
		int trips = 0;
		while (learnedLifeSkillLevel(leatherworker, "HUNTING") < 7) {
			assertTrue(trips++ < 1600, "银档狩猎资格成长超出预算");
			String node = "HUNTING_HIDE_" + (learnedLifeSkillLevel(leatherworker, "HUNTING") >= 6 ? 6 : 5);
			gatherMaterial(leatherworker, quantity(leatherworker, "LIFE_HIDE_2") + 24, "HUNTING", "TOOL_IRON_TRAP",
					"IRON_HIGHLAND", node, "LIFE_HIDE_2", 0,
					() -> replenishIronTools(leatherworker, alchemist, artisan));
			sellManufacturedSurplus(leatherworker, "LIFE_HIDE_2", 20);
		}
		System.out.printf("NATURAL_SILVER_HUNTING level=7 trips=%d%n", trips);
		// 狩猎资格训练本身也产生稀有物，完成训练后再次腾出普通及稀有产出的独立格位。
		prepareManufacturingInventory(leatherworker, java.util.Map.of());
		long goldBefore = queries.find(leatherworker).gold();
		int crafts = 0;
		while (learnedLifeSkillLevel(leatherworker, "LEATHERWORK") < 9) {
			assertTrue(crafts < 5500, "银档制皮成长超出制造预算");
			var recipe = production
				.recipe(learnedLifeSkillLevel(leatherworker, "LEATHERWORK") >= 8 ? "LIFE_WRAP_3_FINE" : "LIFE_WRAP_3");
			if (quantity(leatherworker, "LIFE_HIDE_3") < recipe.ingredients().get("LIFE_HIDE_3")) {
				String node = "HUNTING_HIDE_" + (learnedLifeSkillLevel(leatherworker, "HUNTING") >= 8 ? 8 : 7);
				gatherMaterial(leatherworker, 24, "HUNTING", "TOOL_SILVER_TRAP", "SILVER_FROSTWOOD", node,
						"LIFE_HIDE_3", 0, () -> replenishSilverLeatherworkTool(leatherworker, alchemist, artisan, cook,
								"TOOL_SILVER_TRAP"));
			}
			replenishSilverLeatherworkTool(leatherworker, alchemist, artisan, cook, "TOOL_SILVER_NEEDLE");
			// 批次不越过原有16件整理点，累计次数按服务端真实完成数量计算。
			crafts += craftWithInventory(leatherworker, recipe.id(), "TOOL_SILVER_NEEDLE", "GNOME_LEATHERWORKS",
					Math.min(16 - crafts % 16, 5500 - crafts));
			if (crafts % 16 == 0)
				prepareManufacturingInventory(leatherworker, java.util.Map.of("LIFE_HIDE_3", 24));
			if (crafts % 128 == 0)
				System.out.printf("NATURAL_SILVER_LEATHERWORK_PROGRESS crafts=%d level=%d gold=%d%n", crafts,
						learnedLifeSkillLevel(leatherworker, "LEATHERWORK"), queries.find(leatherworker).gold());
		}
		sellManufacturedSurplus(leatherworker, "LIFE_WRAP_3", 20);
		assertEquals(9, learnedLifeSkillLevel(leatherworker, "LEATHERWORK"));
		System.out.printf("NATURAL_SILVER_LEATHERWORK level=9 crafts=%d goldBefore=%d goldAfter=%d%n", crafts,
				goldBefore, queries.find(leatherworker).gold());
	}

	private void replenishSilverLeatherworkTool(long leatherworker, long alchemist, long artisan, long cook,
			String code) {
		if (quantity(leatherworker, code) > 0)
			return;
		assertTrue(List.of("TOOL_SILVER_NEEDLE", "TOOL_SILVER_TRAP").contains(code));
		// 每次只补缺失的一把；所有二级中间材料不足时仍由原供应角色采集、加工。
		for (long supplier : new long[] { leatherworker, alchemist, artisan }) {
			boolean hides = supplier == leatherworker;
			boolean herbs = supplier == alchemist;
			String product = hides ? "LIFE_WRAP_2" : herbs ? "LIFE_REAGENT_2" : "LIFE_PART_2";
			if (quantity(supplier, product) == 0) {
				String material = hides ? "LIFE_HIDE_2" : herbs ? "LIFE_HERB_2" : "LIFE_ORE_2";
				String skill = hides ? "HUNTING" : herbs ? "HERBALISM" : "MINING";
				String tool = hides ? "TOOL_IRON_TRAP" : herbs ? "TOOL_IRON_PINCERS" : "TOOL_IRON_PICKAXE";
				String node = hides ? "HUNTING_HIDE_5" : herbs ? "HERBALISM_HERB_5" : "MINING_ORE_5";
				gatherMaterial(supplier, production.recipe(product).ingredients().get(material), skill, tool,
						"IRON_HIGHLAND", node, material, 0,
						() -> replenishCooperativeIronTool(leatherworker, alchemist, artisan, supplier, tool));
				String craftTool = hides ? "TOOL_IRON_NEEDLE" : herbs ? "TOOL_IRON_ROD" : "TOOL_IRON_HAMMER";
				if (hides)
					replenishIronTools(leatherworker, alchemist, artisan);
				else
					replenishCooperativeIronTool(leatherworker, alchemist, artisan, supplier, craftTool);
				craftWithInventory(supplier, product, craftTool, production.recipe(product).locations().getFirst());
			}
		}
		completeIronCookingSupply(cook, leatherworker, alchemist, artisan);
		gatherMaterial(artisan, 2, "LOGGING", "TOOL_IRON_AXE", "IRON_HIGHLAND", "LOGGING_WOOD_5", "LIFE_WOOD_2", 0,
				() -> replenishCooperativeIronTool(leatherworker, alchemist, artisan, artisan, "TOOL_IRON_AXE"));
		gatherMaterial(artisan, 2, "MINING", "TOOL_IRON_PICKAXE", "IRON_HIGHLAND", "MINING_ORE_5", "LIFE_ORE_2", 0,
				() -> replenishCooperativeIronTool(leatherworker, alchemist, artisan, artisan, "TOOL_IRON_PICKAXE"));
		tradeManufacturedTool(leatherworker, artisan, "LIFE_WRAP_2");
		tradeManufacturedTool(alchemist, artisan, "LIFE_REAGENT_2");
		tradeManufacturedTool(cook, artisan, "LIFE_RATION_2");
		boolean needle = code.equals("TOOL_SILVER_NEEDLE");
		long maker = needle ? alchemist : artisan;
		if (needle)
			for (var ingredient : production.recipe(code).ingredients().entrySet())
				for (int count = 0; count < ingredient.getValue(); count++)
					tradeManufacturedTool(artisan, maker, ingredient.getKey());
		String craftTool = needle ? "TOOL_IRON_ROD" : "TOOL_IRON_HAMMER";
		replenishCooperativeIronTool(leatherworker, alchemist, artisan, maker, craftTool);
		craftWithInventory(maker, code, craftTool, production.recipe(code).locations().getFirst());
		tradeManufacturedTool(maker, leatherworker, code);
		assertEquals(1, quantity(leatherworker, code));
		System.out.printf("NATURAL_SILVER_REPLENISH product=%s%n", code);
	}

	void completeCopperLeatherworkGrowth(long owner, long alchemist, long artisan) {
		long goldBefore = queries.find(owner).gold();
		int huntingTrips = 0;
		// 制皮先到3级不代表狩猎也已到3级；先用木工具采集二级兽踪取得资格。
		while (lifeSkillQueries.list(owner)
			.stream()
			.filter(s -> s.code().equals("HUNTING"))
			.findFirst()
			.orElseThrow()
			.level() < 3) {
			assertTrue(huntingTrips++ < 30, "狩猎铜档资格成长超出采集预算");
			gatherMaterial(owner, quantity(owner, "LIFE_HIDE_0") + 24, "HUNTING", "TOOL_WOOD_TRAP", "CITY_OUTSKIRTS",
					"HUNTING_2", "LIFE_HIDE_0");
		}
		System.out.printf("NATURAL_COPPER_HUNTING level=3 trips=%d goldBefore=%d goldAfter=%d%n", huntingTrips,
				goldBefore, queries.find(owner).gold());
		int crafts = 0;
		while (lifeSkillQueries.list(owner)
			.stream()
			.filter(s -> s.code().equals("LEATHERWORK"))
			.findFirst()
			.orElseThrow()
			.level() < 5) {
			assertTrue(crafts++ < 400, "铜档制皮升级超出制造预算");
			int level = lifeSkillQueries.list(owner)
				.stream()
				.filter(s -> s.code().equals("LEATHERWORK"))
				.findFirst()
				.orElseThrow()
				.level();
			var recipe = production.recipe(level == 3 ? "LIFE_WRAP_1" : "LIFE_WRAP_1_FINE");
			int huntingLevel = lifeSkillQueries.list(owner)
				.stream()
				.filter(s -> s.code().equals("HUNTING"))
				.findFirst()
				.orElseThrow()
				.level();
			String node = huntingLevel >= 4 ? "HUNTING_HIDE_4" : "HUNTING_HIDE_3";
			gatherMaterial(owner, recipe.ingredients().get("LIFE_HIDE_1"), "HUNTING", "TOOL_COPPER_TRAP",
					"COPPER_WETLAND", node, "LIFE_HIDE_1", 0,
					() -> replenishCopperTools(artisan, owner, "TOOL_COPPER_TRAP"));
			replenishCopperTools(alchemist, owner, "TOOL_COPPER_NEEDLE");
			craftWithInventory(owner, recipe.id(), "TOOL_COPPER_NEEDLE", "GNOME_LEATHERWORKS");
			int afterLevel = lifeSkillQueries.list(owner)
				.stream()
				.filter(s -> s.code().equals("LEATHERWORK"))
				.findFirst()
				.orElseThrow()
				.level();
			if (afterLevel != level)
				System.out.printf("NATURAL_COPPER_LEVEL level=%d crafts=%d gold=%d%n", afterLevel, crafts,
						queries.find(owner).gold());
		}
		System.out.printf("NATURAL_COPPER_LEATHERWORK level=5 crafts=%d goldBefore=%d goldAfter=%d%n", crafts,
				goldBefore, queries.find(owner).gold());
	}

	private void replenishCopperTools(long supplier, long buyer, String tool) {
		while (quantity(buyer, tool) < 5) {
			manufactureCopperTool(supplier, tool);
			tradeManufacturedTool(supplier, buyer, tool);
		}
	}

	void qualifyIronToolSupplier(long owner, String copperRecipe) {
		var recipe = production.recipe(copperRecipe);
		assertTrue(copperRecipe.equals("TOOL_COPPER_NEEDLE") || copperRecipe.equals("TOOL_COPPER_TRAP"));
		long goldBefore = queries.find(owner).gold();
		int crafts = 0;
		while (lifeSkillQueries.list(owner)
			.stream()
			.filter(s -> s.code().equals(recipe.skill()))
			.findFirst()
			.orElseThrow()
			.level() < 5) {
			assertTrue(crafts++ < 600, "铁工具供应专业成长超出制造预算");
			manufactureCopperTool(owner, copperRecipe);
			// 练级产物按正式回收价出售，保留20件合作储备，不以背包扩容绕过库存限制。
			walk(owner, "CITY_SHOP");
			for (var item : items.list(owner)) {
				if (!item.code().equals(copperRecipe) || quantity(owner, copperRecipe) <= 20)
					continue;
				int count = Math.min(item.quantity(), quantity(owner, copperRecipe) - 20);
				lifeShop.sell(owner, new io.github.lishangbu.avalon.world.application.model.LifeShopRequest(
						UUID.randomUUID().toString(), item.id(), count));
			}
			if (crafts % 128 == 0)
				System.out.printf("NATURAL_IRON_QUALIFICATION skill=%s crafts=%d gold=%d%n", recipe.skill(), crafts,
						queries.find(owner).gold());
		}
		System.out.printf("NATURAL_IRON_SUPPLIER skill=%s level=5 crafts=%d goldBefore=%d goldAfter=%d%n",
				recipe.skill(), crafts, goldBefore, queries.find(owner).gold());
	}

	void manufactureCopperTool(long owner, String recipeId) {
		var recipe = production.recipe(recipeId);
		assertTrue(recipeId.startsWith("TOOL_COPPER_"));
		long beforeGold = queries.find(owner).gold();
		walk(owner, "CITY_SHOP");
		// 制药角色以遗忘采药换取伐木名额；制造专业的自然熟练度保留。
		if (lifeSkillQueries.list(owner).stream().anyMatch(s -> s.code().equals("HERBALISM") && s.learned()))
			lifeSkills.change(owner,
					new io.github.lishangbu.avalon.world.application.model.LifeSkillRequest(
							UUID.randomUUID().toString(),
							io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill.HERBALISM, true),
					true);
		gatherWood(owner, recipe.ingredients().get("LIFE_WOOD_0"));
		gatherOre(owner, recipe.ingredients().get("LIFE_ORE_0"));
		String tool = "TOOL_WOOD_"
				+ io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill.valueOf(recipe.skill()).tool;
		craftWithInventory(owner, recipeId, tool, recipe.locations().getFirst());
		System.out.printf("NATURAL_COPPER_TOOL recipe=%s goldBefore=%d goldAfter=%d%n", recipeId, beforeGold,
				queries.find(owner).gold());
	}

	void prepareIronToolMaterials(long leatherworker, long alchemist, long artisan) {
		// 制皮保留狩猎，基础木材由工匠交易提供，不临时突破三个专业名额。
		for (String code : List.of("TOOL_COPPER_HAMMER", "TOOL_COPPER_ROD", "TOOL_COPPER_PINCERS")) {
			gatherWood(artisan, 2);
			tradeManufacturedTool(artisan, leatherworker, "LIFE_WOOD_0");
			tradeManufacturedTool(artisan, leatherworker, "LIFE_WOOD_0");
			gatherOre(leatherworker, 2);
			craftWithInventory(leatherworker, code, "TOOL_WOOD_NEEDLE", "GNOME_LEATHERWORKS");
			tradeManufacturedTool(leatherworker, code.equals("TOOL_COPPER_HAMMER") ? artisan : alchemist, code);
		}
		for (String code : List.of("TOOL_COPPER_PICKAXE", "TOOL_COPPER_AXE", "TOOL_COPPER_TRAP"))
			manufactureCopperTool(artisan, code);
		tradeManufacturedTool(artisan, leatherworker, "TOOL_COPPER_TRAP");
		manufactureCopperTool(alchemist, "TOOL_COPPER_NEEDLE");
		tradeManufacturedTool(alchemist, leatherworker, "TOOL_COPPER_NEEDLE");
		walk(alchemist, "CITY_SHOP");
		if (lifeSkillQueries.list(alchemist).stream().anyMatch(s -> s.code().equals("LOGGING") && s.learned()))
			lifeSkills.change(alchemist,
					new io.github.lishangbu.avalon.world.application.model.LifeSkillRequest(
							UUID.randomUUID().toString(),
							io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill.LOGGING, true),
					true);
		trainGatheringToCopper(artisan, "MINING", "TOOL_WOOD_PICKAXE", "LIFE_ORE_0", "CAVE_ENTRANCE", "MINING_1",
				"GNOME_MINE_GATE", "MINING_2");
		trainGatheringToCopper(artisan, "LOGGING", "TOOL_WOOD_AXE", "LIFE_WOOD_0", "FOREST_EDGE", "LOGGING_1",
				"GNOME_OLD_TRAIL", "LOGGING_2");
		trainGatheringToCopper(alchemist, "HERBALISM", "TOOL_WOOD_PINCERS", "LIFE_HERB_0", "RIVERSIDE", "HERBALISM_1",
				"GNOME_GARDEN", "HERBALISM_2");
		trainGatheringToCopper(leatherworker, "HUNTING", "TOOL_WOOD_TRAP", "LIFE_HIDE_0", "PLAIN_SIGN", "HUNTING_1",
				"CITY_OUTSKIRTS", "HUNTING_2");
		prepareIronToolIngredients(leatherworker, alchemist, artisan);
	}

	private void prepareIronToolIngredients(long leatherworker, long alchemist, long artisan) {
		prepareIronToolIngredients(leatherworker, alchemist, artisan, true, true);
	}

	private void prepareIronToolIngredients(long leatherworker, long alchemist, long artisan, boolean needle,
			boolean trap) {
		int sets = (needle ? 1 : 0) + (trap ? 1 : 0);
		gatherMaterial(artisan, 5 * sets, "MINING", "TOOL_COPPER_PICKAXE", "COPPER_WETLAND", "MINING_ORE_3",
				"LIFE_ORE_1", 0, () -> ensureOwnedCopperTool(artisan, "TOOL_COPPER_PICKAXE"));
		gatherMaterial(artisan, 2 * sets, "LOGGING", "TOOL_COPPER_AXE", "COPPER_WETLAND", "LOGGING_WOOD_3",
				"LIFE_WOOD_1", 0, () -> ensureOwnedCopperTool(artisan, "TOOL_COPPER_AXE"));
		gatherMaterial(alchemist, 3 * sets, "HERBALISM", "TOOL_COPPER_PINCERS", "COPPER_WETLAND", "HERBALISM_HERB_3",
				"LIFE_HERB_1", 0,
				() -> replenishLeatherworkerCopperTool(leatherworker, artisan, alchemist, "TOOL_COPPER_PINCERS"));
		gatherMaterial(leatherworker, 3 * sets, "HUNTING", "TOOL_COPPER_TRAP", "COPPER_WETLAND", "HUNTING_HIDE_3",
				"LIFE_HIDE_1", 0, () -> replenishCopperTools(artisan, leatherworker, "TOOL_COPPER_TRAP"));
		for (int count = 0; count < sets; count++) {
			replenishLeatherworkerCopperTool(leatherworker, artisan, artisan, "TOOL_COPPER_HAMMER");
			replenishLeatherworkerCopperTool(leatherworker, artisan, alchemist, "TOOL_COPPER_ROD");
			replenishCooperativeCopperNeedle(leatherworker, alchemist, artisan);
			craftWithInventory(artisan, "LIFE_PART_1", "TOOL_COPPER_HAMMER", "GNOME_FORGE");
			craftWithInventory(alchemist, "LIFE_REAGENT_1", "TOOL_COPPER_ROD", "GNOME_APOTHECARY");
			craftWithInventory(leatherworker, "LIFE_WRAP_1", "TOOL_COPPER_NEEDLE", "GNOME_LEATHERWORKS");
			tradeManufacturedTool(leatherworker, artisan, "LIFE_WRAP_1");
			tradeManufacturedTool(alchemist, artisan, "LIFE_REAGENT_1");
		}
		if (needle)
			for (var ingredient : production.recipe("TOOL_IRON_NEEDLE").ingredients().entrySet())
				for (int count = 0; count < ingredient.getValue(); count++)
					tradeManufacturedTool(artisan, alchemist, ingredient.getKey());
		System.out.printf("NATURAL_IRON_MATERIALS leatherGold=%d alchemyGold=%d artisanGold=%d%n",
				queries.find(leatherworker).gold(), queries.find(alchemist).gold(), queries.find(artisan).gold());
	}

	void manufactureAndTradeIronTools(long leatherworker, long alchemist, long artisan) {
		manufactureAndTradeIronTools(leatherworker, alchemist, artisan, true, true);
	}

	private void manufactureAndTradeIronTools(long leatherworker, long alchemist, long artisan, boolean needle,
			boolean trap) {
		if (needle) {
			replenishLeatherworkerCopperTool(leatherworker, artisan, alchemist, "TOOL_COPPER_ROD");
			craftWithInventory(alchemist, "TOOL_IRON_NEEDLE", "TOOL_COPPER_ROD", "GNOME_APOTHECARY");
			tradeManufacturedTool(alchemist, leatherworker, "TOOL_IRON_NEEDLE");
		}
		if (trap) {
			replenishLeatherworkerCopperTool(leatherworker, artisan, artisan, "TOOL_COPPER_HAMMER");
			craftWithInventory(artisan, "TOOL_IRON_TRAP", "TOOL_COPPER_HAMMER", "GNOME_FORGE");
			tradeManufacturedTool(artisan, leatherworker, "TOOL_IRON_TRAP");
		}
		assertTrue(quantity(leatherworker, "TOOL_IRON_NEEDLE") > 0);
		assertTrue(quantity(leatherworker, "TOOL_IRON_TRAP") > 0);
		System.out.printf("NATURAL_IRON_DELIVERY needle=%d trap=%d%n", needle ? 1 : 0, trap ? 1 : 0);
	}

	void completeIronLeatherworkOpening(long leatherworker, long alchemist, long artisan) {
		assertEquals(5, learnedLifeSkillLevel(leatherworker, "LEATHERWORK"));
		assertTrue(learnedLifeSkillLevel(alchemist, "ALCHEMY") >= 5);
		assertTrue(learnedLifeSkillLevel(artisan, "ARTISAN") >= 5);
		int huntingTrips = 0;
		while (learnedLifeSkillLevel(leatherworker, "HUNTING") < 5) {
			assertTrue(huntingTrips++ < 400, "狩猎铁档资格成长超出采集预算");
			String node = learnedLifeSkillLevel(leatherworker, "HUNTING") >= 4 ? "HUNTING_HIDE_4" : "HUNTING_HIDE_3";
			gatherMaterial(leatherworker, quantity(leatherworker, "LIFE_HIDE_1") + 24, "HUNTING", "TOOL_COPPER_TRAP",
					"COPPER_WETLAND", node, "LIFE_HIDE_1", 0,
					() -> replenishCopperTools(artisan, leatherworker, "TOOL_COPPER_TRAP"));
			sellManufacturedSurplus(leatherworker, "LIFE_HIDE_1", 20);
		}
		System.out.printf("NATURAL_IRON_HUNTING level=%d trips=%d%n", learnedLifeSkillLevel(leatherworker, "HUNTING"),
				huntingTrips);
		long goldBefore = queries.find(leatherworker).gold();
		int crafts = 0;
		while (learnedLifeSkillLevel(leatherworker, "LEATHERWORK") < 7) {
			assertTrue(crafts < 1400, "铁档制皮成长超出制造预算");
			var recipe = production
				.recipe(learnedLifeSkillLevel(leatherworker, "LEATHERWORK") == 5 ? "LIFE_WRAP_2" : "LIFE_WRAP_2_FINE");
			String node = learnedLifeSkillLevel(leatherworker, "HUNTING") >= 6 ? "HUNTING_HIDE_6" : "HUNTING_HIDE_5";
			// 一次采回少量库存，减少每制造一件便往返资源点；仍逐次支付采集和制造成本。
			int hidesRequired = recipe.ingredients().get("LIFE_HIDE_2");
			if (quantity(leatherworker, "LIFE_HIDE_2") < hidesRequired)
				gatherMaterial(leatherworker, 12, "HUNTING", "TOOL_IRON_TRAP", "IRON_HIGHLAND", node, "LIFE_HIDE_2", 0,
						() -> replenishIronTools(leatherworker, alchemist, artisan));
			if (quantity(leatherworker, "TOOL_IRON_NEEDLE") == 0)
				replenishIronTools(leatherworker, alchemist, artisan);
			crafts += craftWithInventory(leatherworker, recipe.id(), "TOOL_IRON_NEEDLE", "GNOME_LEATHERWORKS",
					Math.min(16 - crafts % 16, 1400 - crafts));
			if (crafts % 16 == 0)
				sellManufacturedSurplus(leatherworker, "LIFE_WRAP_2", 20);
			if (crafts % 128 == 0)
				System.out.printf("NATURAL_IRON_LEATHERWORK_PROGRESS crafts=%d gold=%d%n", crafts,
						queries.find(leatherworker).gold());
		}
		sellManufacturedSurplus(leatherworker, "LIFE_WRAP_2", 20);
		assertEquals(7, learnedLifeSkillLevel(leatherworker, "LEATHERWORK"));
		System.out.printf("NATURAL_IRON_LEATHERWORK level=7 crafts=%d goldBefore=%d goldAfter=%d%n", crafts, goldBefore,
				queries.find(leatherworker).gold());
	}

	void completeIronSupplierGrowth(long leatherworker, long alchemist, long artisan) {
		assertEquals(7, learnedLifeSkillLevel(leatherworker, "LEATHERWORK"));
		assertEquals(5, learnedLifeSkillLevel(alchemist, "ALCHEMY"));
		assertEquals(5, learnedLifeSkillLevel(artisan, "ARTISAN"));
		trainGatheringToIron(artisan, "MINING", "TOOL_COPPER_PICKAXE", "LIFE_ORE_1", "MINING_ORE_",
				() -> ensureOwnedCopperTool(artisan, "TOOL_COPPER_PICKAXE"));
		trainGatheringToIron(alchemist, "HERBALISM", "TOOL_COPPER_PINCERS", "LIFE_HERB_1", "HERBALISM_HERB_",
				() -> replenishLeatherworkerCopperTool(leatherworker, artisan, alchemist, "TOOL_COPPER_PINCERS"));
		for (long supplier : new long[] { artisan, alchemist }) {
			boolean parts = supplier == artisan;
			String skill = parts ? "ARTISAN" : "ALCHEMY";
			String material = parts ? "LIFE_ORE_2" : "LIFE_HERB_2";
			String product = parts ? "LIFE_PART_2" : "LIFE_REAGENT_2";
			String gatheringTool = parts ? "TOOL_IRON_PICKAXE" : "TOOL_IRON_PINCERS";
			String craftingTool = parts ? "TOOL_IRON_HAMMER" : "TOOL_IRON_ROD";
			String gatheringSkill = parts ? "MINING" : "HERBALISM";
			String nodePrefix = parts ? "MINING_ORE_" : "HERBALISM_HERB_";
			int crafts = 0;
			long goldBefore = queries.find(supplier).gold();
			while (learnedLifeSkillLevel(supplier, skill) < 7) {
				assertTrue(crafts < 1400, "铁档供应专业成长超出制造预算：" + skill);
				var recipe = production.recipe(product + (learnedLifeSkillLevel(supplier, skill) >= 6 ? "_FINE" : ""));
				if (quantity(supplier, material) < recipe.ingredients().get(material)) {
					String node = nodePrefix + (learnedLifeSkillLevel(supplier, gatheringSkill) >= 6 ? 6 : 5);
					gatherMaterial(supplier, 12, gatheringSkill, gatheringTool, "IRON_HIGHLAND", node, material, 0,
							() -> replenishCooperativeIronTool(leatherworker, alchemist, artisan, supplier,
									gatheringTool));
				}
				replenishCooperativeIronTool(leatherworker, alchemist, artisan, supplier, craftingTool);
				// 批次在升级、现有原料和每16件整理点处收缩，保持配方切换与库存回收的原有时机。
				// 按服务端实际完成件数累计；工具提前报废后仍由下一轮正式补造，不预置额外工具。
				crafts += craftWithInventory(supplier, recipe.id(), craftingTool, recipe.locations().getFirst(),
						Math.min(16 - crafts % 16, 1400 - crafts));
				if (crafts % 16 == 0)
					sellManufacturedSurplus(supplier, product, 20);
				if (crafts % 128 == 0)
					System.out.printf("NATURAL_IRON_SUPPLIER_PROGRESS skill=%s crafts=%d gold=%d%n", skill, crafts,
							queries.find(supplier).gold());
			}
			sellManufacturedSurplus(supplier, product, 20);
			assertEquals(7, learnedLifeSkillLevel(supplier, skill));
			System.out.printf("NATURAL_IRON_SUPPLIER level=7 skill=%s crafts=%d goldBefore=%d goldAfter=%d%n", skill,
					crafts, goldBefore, queries.find(supplier).gold());
		}
	}

	private void trainGatheringToIron(long owner, String skill, String tool, String material, String nodePrefix,
			Runnable replenishTools) {
		int trips = 0;
		while (learnedLifeSkillLevel(owner, skill) < 5) {
			assertTrue(trips++ < 400, "铁档采集资格成长超出预算：" + skill);
			String node = nodePrefix + (learnedLifeSkillLevel(owner, skill) >= 4 ? 4 : 3);
			gatherMaterial(owner, quantity(owner, material) + 24, skill, tool, "COPPER_WETLAND", node, material, 0,
					replenishTools);
			sellManufacturedSurplus(owner, material, 20);
		}
		System.out.printf("NATURAL_IRON_GATHERING skill=%s level=%d trips=%d%n", skill,
				learnedLifeSkillLevel(owner, skill), trips);
	}

	private void replenishCooperativeIronTool(long leatherworker, long alchemist, long artisan, long buyer,
			String code) {
		if (quantity(buyer, code) > 0)
			return;
		assertTrue(List
			.of("TOOL_IRON_PICKAXE", "TOOL_IRON_PINCERS", "TOOL_IRON_HAMMER", "TOOL_IRON_ROD", "TOOL_IRON_TRAP",
					"TOOL_IRON_POT", "TOOL_IRON_AXE")
			.contains(code));
		// 准备一套真实铁工具原料，先集中到工匠，再交给配方所属的制造者。
		prepareIronToolIngredients(leatherworker, alchemist, artisan, false, true);
		var recipe = production.recipe(code);
		long maker = recipe.skill().equals("ARTISAN") ? artisan : leatherworker;
		if (maker != artisan)
			for (var ingredient : recipe.ingredients().entrySet())
				for (int count = 0; count < ingredient.getValue(); count++)
					tradeManufacturedTool(artisan, maker, ingredient.getKey());
		String tool = maker == artisan ? "TOOL_COPPER_HAMMER" : "TOOL_COPPER_NEEDLE";
		if (maker == artisan)
			replenishLeatherworkerCopperTool(leatherworker, artisan, maker, tool);
		else
			replenishCooperativeCopperNeedle(leatherworker, alchemist, artisan);
		craftWithInventory(maker, code, tool, recipe.locations().getFirst());
		if (maker != buyer)
			tradeManufacturedTool(maker, buyer, code);
		assertEquals(1, quantity(buyer, code));
		System.out.printf("NATURAL_IRON_SUPPLY_TOOL product=%s buyer=%d%n", code, buyer);
	}

	/** 供自然供料助手读取正式专业等级；不写入熟练度或替代学习资格。 */
	int learnedLifeSkillLevel(long owner, String skill) {
		return lifeSkillQueries.list(owner)
			.stream()
			.filter(s -> s.code().equals(skill) && s.learned())
			.mapToInt(s -> s.level())
			.findFirst()
			.orElseThrow();
	}

	private void replenishIronTools(long leatherworker, long alchemist, long artisan) {
		if (quantity(leatherworker, "TOOL_IRON_NEEDLE") > 0 && quantity(leatherworker, "TOOL_IRON_TRAP") > 0)
			return;
		int needlesBefore = quantity(leatherworker, "TOOL_IRON_NEEDLE");
		int trapsBefore = quantity(leatherworker, "TOOL_IRON_TRAP");
		boolean needle = needlesBefore == 0;
		boolean trap = trapsBefore == 0;
		prepareIronToolIngredients(leatherworker, alchemist, artisan, needle, trap);
		manufactureAndTradeIronTools(leatherworker, alchemist, artisan, needle, trap);
		assertEquals(needlesBefore + (needle ? 1 : 0), quantity(leatherworker, "TOOL_IRON_NEEDLE"));
		assertEquals(trapsBefore + (trap ? 1 : 0), quantity(leatherworker, "TOOL_IRON_TRAP"));
		System.out.printf("NATURAL_IRON_REPLENISH needleBefore=%d needleAfter=%d trapBefore=%d trapAfter=%d%n",
				needlesBefore, quantity(leatherworker, "TOOL_IRON_NEEDLE"), trapsBefore,
				quantity(leatherworker, "TOOL_IRON_TRAP"));
	}

	private void replenishCooperativeCopperNeedle(long leatherworker, long alchemist, long artisan) {
		if (quantity(leatherworker, "TOOL_COPPER_NEEDLE") > 0)
			return;
		int herbalismBefore = learnedLifeSkillLevel(alchemist, "HERBALISM");
		// 保留已成长的采药专业；供应木材，避免为铜针反复遗忘采药、重练资格。
		gatherWood(artisan, 2);
		for (int count = 0; count < 2; count++)
			tradeManufacturedTool(artisan, alchemist, "LIFE_WOOD_0");
		gatherOre(alchemist, 2);
		craftWithInventory(alchemist, "TOOL_COPPER_NEEDLE", "TOOL_WOOD_ROD", "GNOME_APOTHECARY");
		tradeManufacturedTool(alchemist, leatherworker, "TOOL_COPPER_NEEDLE");
		assertEquals(herbalismBefore, learnedLifeSkillLevel(alchemist, "HERBALISM"));
	}

	private void sellManufacturedSurplus(long owner, String code, int reserve) {
		if (quantity(owner, code) <= reserve)
			return;
		walk(owner, "CITY_SHOP");
		for (var item : items.list(owner)) {
			if (!item.code().equals(code) || quantity(owner, code) <= reserve)
				continue;
			int count = Math.min(item.quantity(), quantity(owner, code) - reserve);
			lifeShop.sell(owner, new io.github.lishangbu.avalon.world.application.model.LifeShopRequest(
					UUID.randomUUID().toString(), item.id(), count));
		}
	}

	/** 制造阶段切换时通过正式生活商店整理材料；调用者显式指定需要保留的特殊储备。 */
	void prepareManufacturingInventory(long owner, java.util.Map<String, Integer> reserves) {
		prepareMaterialInventory(owner, reserves, 20);
	}

	/** 高阶递归补造临近满包时只保留明确储备，避免每种历史材料保留20份仍占满格子。 */
	void prepareSupplyInventory(long owner, java.util.Map<String, Integer> reserves) {
		// 供应角色也亲自推进地图剧情，会积累不属于生活材料的旧装备。
		// 先按既有规则穿戴更合适的装备，再正式回收落后十级且未穿戴的装备，
		// 避免材料整理后仍然满包；现用装备、工具、图纸和显式原料储备均保留。
		// 城市商店同样支持装备回收，高阶供料不必为整理背包专程返回开篇村庄。
		clearUnusedEquipment(owner, 10, "CITY_SHOP");
		prepareMaterialInventory(owner, reserves, 0);
		// 主线战斗掉落的进化材料不属于生活商品，也没有回收价格；通过公开丢弃命令释放历史占用。
		// 仅处理无显式储备的普通材料，图纸、任务物品、技能书、药剂与工具均不在此范围。
		for (var item : items.list(owner)) {
			if (item.slot() != null || reserves.getOrDefault(item.code(), 0) > 0)
				continue;
			var definition = itemCatalog.item(item.code());
			if (definition.task()
					|| definition.category() != io.github.lishangbu.avalon.content.item.model.ItemCategory.MATERIAL
					|| production.sellPrices().containsKey(item.code()) || item.code().equals("E_FORGE_BLUEPRINT"))
				continue;
			var request = new io.github.lishangbu.avalon.inventory.application.model.EquipmentCommerceRequest(
					UUID.randomUUID().toString(), "DISCARD", null, item);
			equipmentCommerce.execute(owner, request);
			equipmentCommerce.execute(owner, request);
		}
	}

	/**
	 * 联合任务需要同时携带多种物资，不能继续为每一种历史制造材料保留20份。 只保留本阶段提交所需的材料，其余可回收材料按正式价格出售；工具、药剂和任务物品仍保留。
	 * 后续制造缺料时重新采集或向队友采购，让清理产生的收入和再备料成本都进入真实旅程。
	 */
	void prepareJointInventory(long owner) {
		prepareMaterialInventory(owner,
				java.util.Map.of("LIFE_ORE_0", 12, "LIFE_WOOD_0", 8, "LIFE_HERB_0", 12, "LIFE_REAGENT_0", 2,
						"LIFE_PART_0", 3, "LIFE_WRAP_0", 3, "LIFE_ORE_3", 6, "LIFE_PART_3", 4, "LIFE_REAGENT_3", 4),
				0);
	}

	/** 根据阶段储备出售真实余料；默认储备由阶段决定，不能影响不可出售的资产。 */
	private void prepareMaterialInventory(long owner, java.util.Map<String, Integer> reserves, int defaultReserve) {
		// 只整理生活材料及其稀有物，不出售药剂、工具、图纸、任务物品或人物装备。
		// 各类默认保留20份供跨专业补造使用；最终装备原料由调用者提供更高的显式储备。
		for (String code : items.list(owner).stream().map(InventoryItemView::code).distinct().toList()) {
			if (production.sellPrices().containsKey(code)
					&& (code.startsWith("LIFE_") && !code.contains("POTION") || code.startsWith("RARE_")))
				sellManufacturedSurplus(owner, code, reserves.getOrDefault(code, defaultReserve));
		}
		System.out.printf("NATURAL_STAGE_INVENTORY usedSlots=%d capacity=%d%n", items.bag(owner).usedSlots(),
				items.bag(owner).capacity());
	}

	private void ensureOwnedCopperTool(long owner, String code) {
		if (quantity(owner, code) == 0)
			manufactureCopperTool(owner, code);
	}

	private void replenishLeatherworkerCopperTool(long leatherworker, long artisan, long buyer, String code) {
		if (quantity(buyer, code) > 0)
			return;
		gatherWood(artisan, 2);
		for (int count = 0; count < 2; count++)
			tradeManufacturedTool(artisan, leatherworker, "LIFE_WOOD_0");
		gatherOre(leatherworker, 2);
		craftWithInventory(leatherworker, code, "TOOL_WOOD_NEEDLE", "GNOME_LEATHERWORKS");
		tradeManufacturedTool(leatherworker, buyer, code);
	}

	private void trainGatheringToCopper(long owner, String skill, String tool, String material, String firstLocation,
			String firstNode, String secondLocation, String secondNode) {
		int trips = 0;
		while (true) {
			int level = lifeSkillQueries.list(owner)
				.stream()
				.filter(s -> s.code().equals(skill) && s.learned())
				.mapToInt(s -> s.level())
				.findFirst()
				.orElse(1);
			if (level >= 3)
				break;
			assertTrue(trips++ < 80, skill + "铜档采集资格超出预算");
			gatherMaterial(owner, quantity(owner, material) + 12, skill, tool,
					level == 1 ? firstLocation : secondLocation, level == 1 ? firstNode : secondNode, material);
		}
		System.out.printf("NATURAL_COPPER_GATHERING skill=%s level=3 trips=%d%n", skill, trips);
	}

	void tradeManufacturedTool(long seller, long buyer, String code) {
		walk(seller, "CITY_SHOP");
		walk(buyer, "CITY_SHOP");
		marketAccess.teach(seller, true);
		marketAccess.teach(buyer, true);
		var item = items.list(seller)
			.stream()
			.filter(i -> i.code().equals(code) && !i.bound())
			.findFirst()
			.orElseThrow();
		int before = quantity(buyer, code);
		long buyerGoldBefore = queries.find(buyer).gold();
		var listing = listings.listBatch(seller,
				new io.github.lishangbu.avalon.market.application.model.MarketListingBatchRequest(
						UUID.randomUUID().toString(),
						List.of(new io.github.lishangbu.avalon.market.application.model.MarketListingSelection(
								Long.parseLong(item.id()), 1, item)),
						10));
		var request = new io.github.lishangbu.avalon.market.application.model.MarketPurchaseRequest(
				UUID.randomUUID().toString(), Long.parseLong(listing.listingId()), 1, 10);
		var result = purchases.buy(buyer, request);
		assertEquals(before + 1, quantity(buyer, code));
		assertEquals(buyerGoldBefore - 10, queries.find(buyer).gold());
		long sellerGold = queries.find(seller).gold();
		assertEquals(result, purchases.buy(buyer, request));
		assertEquals(sellerGold, queries.find(seller).gold());
		assertEquals(buyerGoldBefore - 10, queries.find(buyer).gold());
		assertEquals(before + 1, quantity(buyer, code));
		System.out.printf("NATURAL_TOOL_TRADE product=%s unitPrice=10 buyerGoldBefore=%d buyerGoldAfter=%d%n", code,
				buyerGoldBefore, queries.find(buyer).gold());
	}

	/** 自然制造按真实堆叠选料，保留工具损耗及同请求重放校验。 */
	void craftWithInventory(long owner, String recipeId, String toolCode, String workshop) {
		assertEquals(1, craftWithInventory(owner, recipeId, toolCode, workshop, 1));
	}

	/** 批量训练按材料、整理边界及升级边界收缩；工具提前耗尽时返回真实完成数，交由原流程补造。 */
	int craftWithInventory(long owner, String recipeId, String toolCode, String workshop, int maximum) {
		assertTrue(maximum > 0 && maximum <= 16);
		long started = System.nanoTime();
		var recipe = production.recipe(recipeId);
		// 单次准备只读取一份背包快照；购买会改变库存，因此购买成功后必须重新读取。
		// 快照不跨制造命令复用，避免工具报废或材料扣除后继续使用过期数量。
		var owned = items.list(owner);
		if (owned.stream().noneMatch(item -> item.code().equals(toolCode))) {
			walk(owner, "CITY_SHOP");
			buyDeficit(owner, toolCode, 1);
			owned = items.list(owner);
		}
		int batch = maximum;
		for (var ingredient : recipe.ingredients().entrySet()) {
			int available = owned.stream()
				.filter(item -> item.code().equals(ingredient.getKey()))
				.mapToInt(item -> item.quantity())
				.sum();
			batch = Math.min(batch, available / ingredient.getValue());
		}
		assertTrue(batch > 0, "缺少真实采集材料：" + recipeId);
		if (batch > 1) {
			var skill = lifeSkillQueries.list(owner)
				.stream()
				.filter(value -> value.code().equals(recipe.skill()))
				.findFirst()
				.orElseThrow();
			long points = Math.round(skill.proficiency() * 2);
			// 仅读取熟练度并按正式奖励规则预估边界；实际熟练度及工具损耗仍逐件结算。
			for (int count = 1; count <= batch; count++) {
				points = io.github.lishangbu.avalon.content.life.LifeSkillRules.gain(points, recipe.level(),
						recipe.points());
				if (io.github.lishangbu.avalon.content.life.LifeSkillRules.level(points) != skill.level()) {
					batch = count;
					break;
				}
			}
		}
		var selected = new java.util.TreeMap<String, Integer>();
		for (var ingredient : recipe.ingredients().entrySet()) {
			int remaining = ingredient.getValue() * batch;
			for (var item : owned) {
				if (!item.code().equals(ingredient.getKey()))
					continue;
				int take = Math.min(remaining, item.quantity());
				if (take > 0)
					selected.put(item.id(), take);
				remaining -= take;
			}
			assertEquals(0, remaining, "缺少真实采集材料：" + ingredient.getKey());
		}
		var tool = owned.stream().filter(i -> i.code().equals(toolCode)).findFirst().orElseThrow();
		walk(owner, workshop);
		// 记录正式命令前后的金币和工具数量，供长路线复盘费用及报废补造；不改变制造结算。
		long craftGoldBefore = queries.find(owner).gold();
		int toolsBefore = owned.stream()
			.filter(item -> item.code().equals(toolCode))
			.mapToInt(item -> item.quantity())
			.sum();
		var request = new io.github.lishangbu.avalon.world.application.model.LifeCraftingRequest(
				UUID.randomUUID().toString(), recipeId, batch, tool.id(), Math.min(tool.quantity(), batch), selected);
		// 分别记录准备、正式结算及请求重放耗时，先用证据定位瓶颈，不减少幂等校验范围。
		long prepared = System.nanoTime();
		var result = crafting.craft(owner, request);
		long settled = System.nanoTime();
		assertTrue(result.completed() > 0 && result.completed() <= batch);
		assertTrue(result.stop().equals("COMPLETED") || result.stop().equals("TOOLS_EXHAUSTED"));
		assertEquals(result, crafting.craft(owner, request));
		long replayed = System.nanoTime();
		System.out.printf("NATURAL_COST_CRAFT owner=%d recipe=%s goldSpent=%d tool=%s toolsLost=%d completed=%d%n",
				owner, recipeId, craftGoldBefore - queries.find(owner).gold(), toolCode,
				toolsBefore - quantity(owner, toolCode), result.completed());
		System.out.printf(
				"NATURAL_PERF_CRAFT recipe=%s prepareMicros=%d settleMicros=%d replayMicros=%d totalMicros=%d%n",
				recipeId, (prepared - started) / 1000, (settled - prepared) / 1000, (replayed - settled) / 1000,
				(System.nanoTime() - started) / 1000);
		return result.completed();
	}

	private void completeBasicManufacturingGrowth(long owner, int targetLevel, String skill, String baseRecipe,
			String material, String gatheringSkill, String gatheringTool, String gatheringLocation,
			String gatheringNode, String craftingTool, String workshop) {
		assertTrue(targetLevel >= 1 && targetLevel <= 3);
		clearUnusedEquipment(owner, 0);
		// 长剧情留下的生产原料可能跨多格；正式回收多余库存，为工具和采集产出留位。
		for (String code : items.list(owner).stream().map(InventoryItemView::code).distinct().toList()) {
			if (production.sellPrices().containsKey(code)
					&& (code.startsWith("LIFE_") && !code.contains("POTION") || code.startsWith("RARE_")))
				sellManufacturedSurplus(owner, code, 20);
		}
		System.out.printf("NATURAL_MANUFACTURING_BAG usedSlots=%d capacity=%d%n", items.bag(owner).usedSlots(),
				items.bag(owner).capacity());
		walk(owner, "CITY_SHOP");
		if (lifeSkillQueries.list(owner).stream().anyMatch(s -> s.code().equals("LOGGING") && s.learned()))
			lifeSkills.change(owner,
					new io.github.lishangbu.avalon.world.application.model.LifeSkillRequest(
							UUID.randomUUID().toString(),
							io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill.LOGGING, true),
					true);
		lifeSkills.change(owner,
				new io.github.lishangbu.avalon.world.application.model.LifeSkillRequest(UUID.randomUUID().toString(),
						io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill.valueOf(skill), false),
				false);
		long goldBefore = queries.find(owner).gold();
		int crafts = 0;
		while (lifeSkillQueries.list(owner)
			.stream()
			.filter(s -> s.code().equals(skill))
			.findFirst()
			.orElseThrow()
			.level() < targetLevel) {
			assertTrue(crafts++ < 100, skill + "起步升级超出制造预算");
			int currentLevel = lifeSkillQueries.list(owner)
				.stream()
				.filter(s -> s.code().equals(skill))
				.findFirst()
				.orElseThrow()
				.level();
			var recipe = production.recipe(currentLevel == 1 ? baseRecipe : baseRecipe + "_FINE");
			int required = recipe.ingredients().get(material);
			gatherMaterial(owner, required, gatheringSkill, gatheringTool, gatheringLocation, gatheringNode, material);
			walk(owner, "CITY_SHOP");
			buyDeficit(owner, craftingTool, 1);
			var selected = new java.util.TreeMap<String, Integer>();
			int remaining = required;
			for (var item : items.list(owner)) {
				if (!item.code().equals(material))
					continue;
				int take = Math.min(remaining, item.quantity());
				if (take > 0)
					selected.put(item.id(), take);
				remaining -= take;
			}
			assertEquals(0, remaining);
			var tool = items.list(owner).stream().filter(i -> i.code().equals(craftingTool)).findFirst().orElseThrow();
			walk(owner, workshop);
			var result = crafting.craft(owner,
					new io.github.lishangbu.avalon.world.application.model.LifeCraftingRequest(
							UUID.randomUUID().toString(), recipe.id(), 1, tool.id(), 1, selected));
			assertEquals(1, result.completed());
		}
		System.out.printf("NATURAL_MANUFACTURING skill=%s level=%d crafts=%d goldBefore=%d goldAfter=%d%n", skill,
				targetLevel, crafts, goldBefore, queries.find(owner).gold());
	}

	void completeLevelCap(long owner) {
		var beforeCap = queries.find(owner);
		trainToLevel(owner, 50, "E_RESIDUAL_FIELD", "E_RESIDUAL_PATROL", 250);
		var afterCap = queries.find(owner);
		assertEquals(50, afterCap.level());
		System.out.printf(
				"POSTGAME_LEVEL_CAP levelBefore=%d experienceBefore=%d experienceAfter=%d goldBefore=%d goldAfter=%d%n",
				beforeCap.level(), beforeCap.experience(), afterCap.experience(), beforeCap.gold(), afterCap.gold());
	}

	private void completeRareCollection(long owner) {
		prepareRareCollectionInventory(owner);
		String city = "SALAMANDER_CITY";
		accept(owner, "E011");
		walk(owner, "E_RESIDUAL_FIELD");
		walk(owner, city);
		deliver(owner, "E011");
		accept(owner, "E012");
		walk(owner, "CITY_SHOP");
		prepareRareCollectionSupplies(owner);
		walk(owner, city);
		int balls = quantity(owner, "SEAL_V");
		deliver(owner, "E012");
		assertEquals(balls, quantity(owner, "SEAL_V"));
		accept(owner, "E013");
		captureEncounter(owner, city, "E_RARE_HABITAT", "E_RARE_FOX", "SEAL_V");
		walk(owner, city);
		deliver(owner, "E013");
		var owned = pets.list(owner).stream().map(p -> p.id()).sorted().toList();
		claim(owner, "E014");
		assertEquals(owned, pets.list(owner).stream().map(p -> p.id()).sorted().toList());
		int remaining = quantity(owner, "SEAL_V");
		claim(owner, "E015");
		assertEquals(remaining + 6, quantity(owner, "SEAL_V"));
		assertEquals(0, quantity(owner, "E_COLLECTION_RECORD"));
		assertEquals(owned, pets.list(owner).stream().map(p -> p.id()).sorted().toList());
	}

	/** 收藏准备必须同时释放历史余料并返回接取城市，避免商店整理改变后续任务地点。 */
	void prepareRareCollectionInventory(long owner) {
		// 收藏不需要生活材料；后续制造仍须真实重新备料，不删除库存或扩大背包容量。
		clearOutgrownEquipment(owner);
		prepareMaterialInventory(owner, java.util.Map.of(), 0);
		walk(owner, "SALAMANDER_CITY");
	}

	/** 稀有收藏补给入口，供局部回归通过真实购买验证背包和金币变化。 */
	void prepareRareCollectionSupplies(long owner) {
		// 前序捕捉可能已持有足量封印球；20是目标库存，不能每次额外购买20个。
		while (quantity(owner, "SEAL_V") < 20)
			battles.purchase(owner, "SEAL_V", UUID.randomUUID().toString());
	}

	/** 主线后回访各城捕捉线；高价值展示只核对拥有，不产生捕获事件要求。 */
	void completeCityCollections(long owner) {
		clearOutgrownEquipment(owner);
		for (String prefix : List.of("U", "S", "F")) {
			String city = prefix.equals("U") ? "UNDINE_CITY" : prefix.equals("S") ? "SYLPH_CITY" : "SALAMANDER_CITY";
			String field = prefix.equals("U") ? "UNDINE_SHALLOWS"
					: prefix.equals("S") ? "SYLPH_RIDGE_ROAD" : "SALAMANDER_ASH_ROAD";
			String entry = prefix.equals("U") ? "UNDINE_ANCIENT_INLET"
					: prefix.equals("S") ? "SYLPH_OLD_POST" : "SALAMANDER_SEALED_WORKS";
			for (int number = 31; number <= 40; number++) {
				String step = "%s%03d".formatted(prefix, number);
				if (questQueries.done(owner, step))
					continue;
				walk(owner, city);
				if (number == 40)
					captureEncounter(owner, city, entry, prefix + "_CAPTURE_40", "SEAL_V");
				walk(owner, city);
				accept(owner, step);
				var owned = pets.list(owner).stream().map(p -> p.id()).sorted().toList();
				if (number == 33) {
					walk(owner, "CITY_SHOP");
					String seals = prefix.equals("F") ? "SEAL_IV" : "SEAL_II";
					while (quantity(owner, seals) < 3)
						battles.purchase(owner, seals, UUID.randomUUID().toString());
				}
				else if (number == 37 && !prefix.equals("S")) {
					for (int fight = 0; fight < (prefix.equals("U") ? 2 : 3); fight++)
						recoveredFight(owner, entry, prefix.equals("U") ? "U_RELIC_GUARD" : "F_RELIC_PATROL");
				}
				else if (number != 35 && number != 40)
					captureEncounter(owner, city, number < 36 ? field : entry, prefix + "_CAPTURE_" + number, "SEAL_V");
				walk(owner, city);
				deliver(owner, step);
				if (number == 35 || number == 40)
					assertEquals(owned, pets.list(owner).stream().map(p -> p.id()).sorted().toList());
			}
		}
		for (var route : List.of(List.of("U046", "UNDINE_CITY", "UNDINE_ANCIENT_INLET", "U_VAULT_FIELDWORK"),
				List.of("S056", "SYLPH_CITY", "SYLPH_OLD_POST", "S_VAULT_FIELDWORK"),
				List.of("F046", "SALAMANDER_CITY", "SALAMANDER_SEALED_WORKS", "F_CAPTURE_46"))) {
			walk(owner, route.get(1));
			accept(owner, route.getFirst());
			captureEncounter(owner, route.get(1), route.get(2), route.get(3), "SEAL_V");
			walk(owner, route.get(1));
			deliver(owner, route.getFirst());
		}
		completeArmorySide(owner);
	}

	private void completeArmorySide(long owner) {
		String city = "SALAMANDER_CITY", entrance = "F_ARMORY_ENTRANCE";
		walk(owner, city);
		accept(owner, "F056");
		captureEncounter(owner, city, entrance, "F_CAPTURE_56", "SEAL_V");
		walk(owner, city);
		deliver(owner, "F056");
		accept(owner, "F057");
		recoveredFight(owner, entrance, "F_ARMORY_GATEKEEPER");
		walk(owner, city);
		deliver(owner, "F057");
		accept(owner, "F058");
		for (int fight = 0; quantity(owner, "F_ARMORY_PART") < 6 && fight < 5; fight++)
			recoveredFight(owner, entrance, "F_ARMORY_PATROL");
		walk(owner, city);
		int parts = quantity(owner, "F_ARMORY_PART");
		deliver(owner, "F058");
		assertEquals(parts - 6, quantity(owner, "F_ARMORY_PART"));
		accept(owner, "F059");
		battles.recover(owner);
		equip(owner);
		walk(owner, entrance);
		completeDungeon(owner, "F_ARMORY", null, 0, 1);
		walk(owner, city);
		deliver(owner, "F059");
		accept(owner, "F060");
		captureEncounter(owner, city, entrance, "F_CAPTURE_60", "SEAL_V");
		walk(owner, city);
		deliver(owner, "F060");
		assertEquals(1, quantity(owner, "F_ARMORY_BLUEPRINT"), () -> "武库图纸未进入背包；pending=" + items.pending(owner));
	}

	private void prepareCapture(long owner, String city, String location, String ball) {
		walk(owner, "CITY_SHOP");
		while (quantity(owner, ball) < 12)
			battles.purchase(owner, ball, UUID.randomUUID().toString());
		if (!city.equals("SALAMANDER_CITY"))
			while (quantity(owner, "HEALTH_POTION") < 12)
				battles.purchase(owner, "HEALTH_POTION", UUID.randomUUID().toString());
		walk(owner, city);
		if (city.equals("SALAMANDER_CITY"))
			buyDeficit(owner, "LIFE_HEALTH_POTION_4", 12);
		battles.recover(owner);
		walk(owner, location);
	}

	/** 捕捉与战败使用正式成本；依公开血量削弱、治疗、换位，不修改战局种子或抽样结果。 */
	private void captureEncounter(long owner, String city, String location, String encounter, String ball) {
		prepareCapture(owner, city, location, ball);
		var battle = battles.start(owner, encounter, UUID.randomUUID().toString());
		for (int attempt = 0; attempt < 3 && !battle.status().equals("WON"); attempt++) {
			if (attempt > 0) {
				assertTrue(List.of("LOST", "SURRENDERED").contains(battle.status()), "捕捉未在预算回合内结束：" + battle.log());
				prepareCapture(owner, city, location, ball);
				battle = battles.start(owner, encounter, UUID.randomUUID().toString());
			}
			for (int round = 0; battle.status().equals("ACTIVE") && round < 80; round++) {
				var replacements = replacements(battle.units());
				var active = battle.units()
					.stream()
					.filter(u -> !u.enemy() && u.hp() > 0
							&& (u.hero() || u.position() < 2 || replacements.containsKey(u.id())))
					.toList();
				var foe = battle.units().stream().filter(u -> u.enemy() && u.hp() > 0).findFirst().orElseThrow();
				var injured = active.stream()
					.min(Comparator.comparingDouble(u -> (double) u.hp() / u.stats().health()))
					.orElseThrow();
				var hero = active.stream().filter(u -> u.hero()).findFirst().orElse(null);
				if (hero == null || hero.actions().stream().noneMatch(a -> a.id().equals(ball) && a.available())) {
					battle = BattleTestCommands.turn(battles, owner, Long.parseLong(battle.id()), battle.round(),
							List.of(), true);
					break;
				}
				String potion = hero == null ? null
						: hero.actions()
							.stream()
							.filter(a -> a.id().contains("HEALTH_POTION") && a.available())
							.map(a -> a.id())
							.max(String::compareTo)
							.orElse(null);
				// 人物负责捕捉，不能因伙伴受伤而连续喂药、始终没有投掷机会。
				boolean usePotion = potion != null && hero.hp() < hero.stats().health() * 0.35;
				String healer = injured.hp() >= injured.stats().health() * 0.8 ? null
						: active.stream()
							.filter(u -> !u.hero())
							.filter(u -> u.actions().stream().anyMatch(a -> a.id().equals("微疗") && a.available()))
							.map(u -> u.id())
							.findFirst()
							.orElse(null);
				var orders = active.stream().map(unit -> {
					if (unit.id().equals(healer))
						return new BattleStateOrder(unit.id(), "微疗", injured.id());
					if (!unit.hero())
						return new BattleStateOrder(unit.id(), "DEFEND", unit.id());
					if (usePotion)
						return new BattleStateOrder(unit.id(), potion, hero.id());
					boolean weaken = unit.level() <= foe.level() + 5 && foe.hp() > foe.stats().health() * 0.6
							&& unit.stats().physicalAttack() < foe.hp() * 0.25;
					return new BattleStateOrder(unit.id(), weaken ? "ATTACK" : ball, foe.id());
				}).toList();
				battle = BattleTestCommands.turn(battles, owner, Long.parseLong(battle.id()), battle.round(), orders,
						false, replacements);
			}
			if (battle.status().equals("ACTIVE"))
				battle = BattleTestCommands.turn(battles, owner, Long.parseLong(battle.id()), battle.round(), List.of(),
						true);
			if (!battle.status().equals("WON"))
				if (Boolean.getBoolean("avalon.test.trace"))
					System.out.printf("NATURAL_CAPTURE_DEFEAT encounter=%s attempt=%d gold=%d%n", encounter,
							attempt + 1, queries.find(owner).gold());
		}
		assertEquals("WON", battle.status(), "捕捉必须通过真实概率完成：" + battle.log());
		assertTrue(battle.units().stream().anyMatch(u -> u.enemy() && u.captured()),
				"获胜不能代替实际捕获：" + encounter + " " + battle.log());
		if (Boolean.getBoolean("avalon.test.trace"))
			System.out.printf("NATURAL_CAPTURE encounter=%s ball=%s gold=%d%n", encounter, ball,
					queries.find(owner).gold());
	}

	/** 通过正式商店仅购买缺额；不能为商店未出售的材料或高档工具补发资产。 */
	void buyDeficit(long owner, String code, int required) {
		int missing = required - quantity(owner, code);
		if (missing <= 0)
			return;
		lifeShop.buy(owner, new io.github.lishangbu.avalon.world.application.model.LifeShopRequest(
				UUID.randomUUID().toString(), code, missing));
		assertTrue(quantity(owner, code) >= required);
	}

	private void clearOutgrownEquipment(long owner) {
		clearUnusedEquipment(owner, 10);
	}

	private void clearUnusedEquipment(long owner, int levelGap) {
		clearUnusedEquipment(owner, levelGap, "WEAPON_SHOP");
	}

	/** 调用阶段选择已开放且支持装备回收的商店，装备筛选及正式出售规则保持一致。 */
	private void clearUnusedEquipment(long owner, int levelGap, String shop) {
		walk(owner, shop);
		equip(owner);
		int sold = 0;
		for (var item : items.list(owner)) {
			if (item.slot() == null || item.equipped() || item.requiredLevel() > queries.find(owner).level() - levelGap)
				continue;
			equipmentCommerce.execute(owner,
					new io.github.lishangbu.avalon.inventory.application.model.EquipmentCommerceRequest(
							UUID.randomUUID().toString(), item.sellPrice() > 0 ? "SELL" : "DESTROY", null, item));
			sold++;
		}
		if (Boolean.getBoolean("avalon.test.trace"))
			System.out.printf("NATURAL_BAG clearedEquipment=%d remainingStacks=%d gold=%d%n", sold,
					items.list(owner).size(), queries.find(owner).gold());
	}

	/** 按真实道路和任务事实寻找路线；自然供料助手不能直接修改角色位置。 */
	void walk(long owner, String target) {
		String current = queries.find(owner).location();
		if (current.equals(target))
			return;
		var completed = facts.codes(owner);
		var queue = new java.util.ArrayDeque<List<String>>();
		var visited = new java.util.HashSet<String>();
		queue.add(List.of(current));
		visited.add(current);
		while (!queue.isEmpty()) {
			var path = queue.remove();
			if (path.getLast().equals(target)) {
				for (String next : path.subList(1, path.size()))
					move(owner, next);
				return;
			}
			for (String next : world.place(path.getLast()).exits()) {
				if (!world.accessible(world.place(next), completed::contains) || !visited.add(next))
					continue;
				var extended = new java.util.ArrayList<>(path);
				extended.add(next);
				queue.add(extended);
			}
		}
		throw new AssertionError("没有已开放路线：" + current + " -> " + target);
	}

	private void gatherOre(long owner) {
		gatherOre(owner, 6);
	}

	private void gatherOre(long owner, int required) {
		gatherMaterial(owner, required, "MINING", "TOOL_WOOD_PICKAXE", "CAVE_ENTRANCE", "MINING_1", "LIFE_ORE_0");
	}

	private void gatherWood(long owner, int required) {
		gatherMaterial(owner, required, "LOGGING", "TOOL_WOOD_AXE", "FOREST_EDGE", "LOGGING_1", "LIFE_WOOD_0");
	}

	private void gatherMaterial(long owner, int required, String skillCode, String toolCode, String location,
			String node, String material) {
		gatherMaterial(owner, required, skillCode, toolCode, location, node, material, 0,
				() -> buyDeficit(owner, toolCode, 5));
	}

	void gatherMaterial(long owner, int required, String skillCode, String toolCode, String location, String node,
			String material, int retries, Runnable replenishTools) {
		assertTrue(retries < 10, "采集补购工具超过重试预算");
		if (quantity(owner, material) >= required)
			return;
		walk(owner, "CITY_SHOP");
		if (lifeSkillQueries.list(owner).stream().noneMatch(skill -> skill.code().equals(skillCode) && skill.learned()))
			lifeSkills.change(owner,
					new io.github.lishangbu.avalon.world.application.model.LifeSkillRequest(
							UUID.randomUUID().toString(),
							io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill.valueOf(skillCode), true),
					false);
		replenishTools.run();
		walk(owner, "CITY_PETS");
		battles.recover(owner);
		walk(owner, location);
		var tool = items.list(owner)
			.stream()
			.filter(i -> i.code().equals(toolCode))
			.max(Comparator.comparingInt(i -> i.quantity()))
			.orElseThrow();
		var pet = pets.list(owner).stream().filter(p -> p.partyPosition() == 0).findFirst().orElseThrow();
		// 按派遣记录实际完成回合与金币扣费；工具耗尽后的重派单独记账，避免递归重复累计。
		long gatherGoldBefore = queries.find(owner).gold();
		var session = gathering.begin(owner, new io.github.lishangbu.avalon.world.application.model.GatheringStart(
				UUID.randomUUID().toString(), node, pet.id(), tool.id(), tool.quantity()));
		int owned = quantity(owner, material);
		long heartbeatNanos = 0, inventoryNanos = 0;
		int heartbeats = 0, inventoryReads = 0;
		for (int second = 0; owned < required && second < 240; second++) {
			if (session.status().equals("TOOLS_EXHAUSTED") || session.status().equals("RESOURCES_LOW")) {
				// 正式停止派遣后递归回城：入口会执行幻兽中心恢复和按缺额补造，保留实际损耗与路程。
				// 背包满等其他停止原因仍直接暴露，不能用重派掩盖库存问题。
				gathering.stop(owner, new io.github.lishangbu.avalon.world.application.model.GatheringStop(
						UUID.randomUUID().toString(), session.id()));
				System.out.printf(
						"NATURAL_COST_GATHER owner=%d node=%s rounds=%d goldSpent=%d exhausted=%s reason=%s%n", owner,
						node, session.completed(), gatherGoldBefore - queries.find(owner).gold(),
						session.status().equals("TOOLS_EXHAUSTED"), session.status());
				System.out.printf(
						"NATURAL_PERF_GATHER heartbeats=%d heartbeatMicros=%d inventoryReads=%d inventoryMicros=%d%n",
						heartbeats, heartbeatNanos / 1000, inventoryReads, inventoryNanos / 1000);
				gatherMaterial(owner, required, skillCode, toolCode, location, node, material, retries + 1,
						replenishTools);
				return;
			}
			assertEquals("ACTIVE", session.status(), () -> "采集停止，应保留真实资源约束：" + material + " bag=" + items.bag(owner));
			// 自然旅程每次推进一秒LifeClock；心跳仍执行正式间隔和资源扣费，无需真实等待。
			long completedBefore = session.completed();
			long started = System.nanoTime();
			session = gathering.heartbeat(owner, new io.github.lishangbu.avalon.world.application.model.GatheringBeat(
					session.id(), session.revision(), true));
			heartbeatNanos += System.nanoTime() - started;
			heartbeats++;
			// 只有完成回合才可能结算产物；空心跳仍正式执行，但无需重复读取完整背包。
			if (session.completed() != completedBefore) {
				started = System.nanoTime();
				owned = quantity(owner, material);
				inventoryNanos += System.nanoTime() - started;
				inventoryReads++;
			}
		}
		gathering.stop(owner, new io.github.lishangbu.avalon.world.application.model.GatheringStop(
				UUID.randomUUID().toString(), session.id()));
		System.out.printf("NATURAL_COST_GATHER owner=%d node=%s rounds=%d goldSpent=%d exhausted=false%n", owner, node,
				session.completed(), gatherGoldBefore - queries.find(owner).gold());
		System.out.printf("NATURAL_PERF_GATHER heartbeats=%d heartbeatMicros=%d inventoryReads=%d inventoryMicros=%d%n",
				heartbeats, heartbeatNanos / 1000, inventoryReads, inventoryNanos / 1000);
		if (quantity(owner, material) < required) {
			// 空产出仍支付资源和工具消耗；单趟预算结束后恢复再采，不要求随机产出必然足量。
			assertEquals("ACTIVE", session.status(), () -> "采集结束仍缺少材料：" + material + " bag=" + items.bag(owner));
			System.out.printf("NATURAL_GATHER_CONTINUE material=%s owned=%d required=%d retry=%d%n", material,
					quantity(owner, material), required, retries + 1);
			gatherMaterial(owner, required, skillCode, toolCode, location, node, material, retries + 1, replenishTools);
			return;
		}
		walk(owner, "CITY_SHOP");
	}

	private void mineTrip(long owner) {
		move(owner, "GNOME_CITY", "CITY_PETS");
		battles.recover(owner);
		move(owner, "GNOME_CITY", "CITY_OUTSKIRTS", "NOM_MINE_APPROACH");
		fight(owner, "NOM_MINE_PATROL");
		move(owner, "CITY_OUTSKIRTS", "GNOME_CITY", "CITY_SHOP");
	}

	private void riverside(long owner) {
		move(owner, "SQUARE", "STONE_PATH", "RIVERSIDE");
		fight(owner, "OPENING_RIVERSIDE_PATROL");
		move(owner, "STONE_PATH", "SQUARE", "PET_CENTER");
		battles.recover(owner);
	}

	private void equip(long owner) {
		var character = queries.find(owner);
		var selected = new java.util.LinkedHashMap<String, InventoryItemView>();
		items.list(owner)
			.stream()
			.filter(item -> item.slot() != null && item.requiredLevel() <= character.level())
			.filter(item -> item.requiredProfession() == null
					|| item.requiredProfession().equals(character.profession()))
			.sorted(Comparator.comparingInt(item -> item.stats().values().stream().mapToInt(Integer::intValue).sum()))
			.forEach(item -> selected.put(item.slot(), item));
		for (var item : selected.values())
			if (!item.equipped())
				inventory.equip(owner, Long.parseLong(item.id()));
	}

	private void move(long owner, String... route) {
		for (String location : route)
			characters.move(owner, location, queries.find(owner).revision());
	}

	/** 使用当前角色修订号显式接取，保留任务前置、实例和奖励规则。 */
	void accept(long owner, String step) {
		acceptance.accept(owner, step,
				new QuestAcceptRequest(UUID.randomUUID().toString(), queries.find(owner).revision()));
	}

	private void claim(long owner, String step) {
		accept(owner, step);
		deliver(owner, step);
	}

	/** 正式交付并重放原结果，验证经验和金币不重复发放。 */
	void deliver(long owner, String step) {
		var first = quests.interact(owner, step);
		var before = queries.find(owner);
		assertEquals(first, quests.interact(owner, step));
		assertEquals(before.experience(), queries.find(owner).experience());
		assertEquals(before.gold(), queries.find(owner).gold());
		if (Boolean.getBoolean("avalon.test.trace"))
			System.out.printf("NATURAL_STORY step=%s level=%d xp=%d gold=%d%n", step, before.level(),
					before.experience(), before.gold());
	}

	private java.util.Map<String, Integer> replacements(
			List<io.github.lishangbu.avalon.battle.application.model.BattleViewUnitView> units) {
		var reserves = units.stream()
			.filter(u -> !u.enemy() && !u.hero() && u.hp() > 0 && u.position() >= 2 && u.position() <= 3)
			.toList();
		var result = new java.util.LinkedHashMap<String, Integer>();
		int next = 0;
		for (int position = 0; position < 2 && next < reserves.size(); position++) {
			int slot = position;
			if (units.stream().noneMatch(u -> !u.enemy() && !u.hero() && u.hp() > 0 && u.position() == slot))
				result.put(reserves.get(next++).id(), position);
		}
		return result;
	}

	private void fight(long owner, String encounter) {
		String location = queries.find(owner).location();
		for (int attempt = 0; attempt < 3; attempt++) {
			if (fightOnce(owner, encounter))
				return;
			if (Boolean.getBoolean("avalon.test.trace"))
				System.out.printf("NATURAL_DEFEAT encounter=%s attempt=%d gold=%d%n", encounter, attempt + 1,
						queries.find(owner).gold());
			if (attempt < 2) {
				String recovery = location.startsWith("UNDINE_") || location.startsWith("U_") ? "UNDINE_CITY"
						: location.startsWith("SYLPH_") || location.startsWith("S_") ? "SYLPH_CITY"
								: location.startsWith("SALAMANDER_") || location.startsWith("F_")
										|| location.startsWith("J_") ? "SALAMANDER_CITY" : "CITY_PETS";
				walk(owner, recovery);
				battles.recover(owner);
				equip(owner);
				walk(owner, location);
			}
		}
		throw new AssertionError("自然路线连续三场未获胜：" + encounter);
	}

	private boolean fightOnce(long owner, String encounter) {
		var battle = battles.start(owner, encounter, UUID.randomUUID().toString());
		if (encounter.equals("NOM_YARD_PATROL")) {
			var enemies = battle.units().stream().filter(u -> u.enemy()).toList();
			assertEquals(2, enemies.size(), "堆场自然验收必须保留双怪，不能以单怪通过替代");
			assertTrue(enemies.stream().allMatch(u -> u.level() == 10), "堆场敌人应使用10级区域模板");
		}
		// 后期精英存在蓄力防御与治疗回合，不能沿用开篇40回合预算。
		for (int n = 0; battle.status().equals("ACTIVE") && n < 80; n++) {
			var replacements = replacements(battle.units());
			var party = battle.units()
				.stream()
				.filter(u -> !u.enemy() && u.hp() > 0
						&& (u.hero() || u.position() < 2 || replacements.containsKey(u.id())))
				.toList();
			var injuredCandidate = party.stream()
				.min(Comparator.comparingDouble(u -> (double) u.hp() / u.stats().health()))
				.orElseThrow();
			var hero = party.stream().filter(u -> u.hero()).findFirst().orElse(null);
			var injured = hero != null && hero.hp() < hero.stats().health() * 0.55 ? hero : injuredCandidate;
			boolean warning = battle.telegraph() != null && battle.telegraph().contains("即将");
			boolean firePack = battle.units().stream().filter(u -> u.enemy() && u.hp() > 0).count() > 1
					&& battle.units()
						.stream()
						.filter(u -> u.enemy() && u.hp() > 0)
						.allMatch(u -> u.element().equals("火"));
			String target = battle.units().stream().filter(u -> u.enemy() && u.hp() > 0).findFirst().orElseThrow().id();
			var orders = party.stream().map(unit -> {
				boolean guard = warning || firePack && unit.hero();
				String action = guard ? "DEFEND" : "ATTACK";
				String aim = guard ? unit.id() : target;
				if (!warning && injured.hp() < injured.stats().health() * 0.7
						&& unit.actions().stream().anyMatch(a -> a.id().equals("微疗") && a.available())) {
					action = "微疗";
					aim = injured.id();
				}
				else if (!warning && unit.hero() && injured.hp() < injured.stats().health() * 0.8
						&& unit.actions().stream().anyMatch(a -> a.id().equals("治疗术") && a.available())) {
					action = "治疗术";
					aim = injured.id();
				}
				else if (!warning && unit.hero() && injured.hp() < injured.stats().health() * 0.6
						&& unit.actions().stream().anyMatch(a -> a.id().contains("HEALTH_POTION") && a.available())) {
					action = unit.actions()
						.stream()
						.filter(a -> a.id().contains("HEALTH_POTION") && a.available())
						.map(a -> a.id())
						.findFirst()
						.orElseThrow();
					aim = injured.id();
				}
				else if (!guard) {
					action = unit.actions()
						.stream()
						.filter(a -> a.available() && a.mana() > 0 && a.target().equals("ENEMY"))
						.map(a -> a.id())
						.findFirst()
						.orElse("ATTACK");
				}
				return new BattleStateOrder(unit.id(), action, aim);
			}).toList();
			battle = BattleTestCommands.turn(battles, owner, Long.parseLong(battle.id()), battle.round(), orders, false,
					replacements);
		}
		assertFalse(battle.status().equals("ACTIVE"), encounter + ": " + battle.log());
		if (!battle.status().equals("WON"))
			if (Boolean.getBoolean("avalon.test.trace"))
				System.out.printf("NATURAL_DEFEAT_LOG encounter=%s log=%s%n", encounter, battle.log());
		return battle.status().equals("WON");
	}

}
