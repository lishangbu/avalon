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

/** 新号仅以正式任务、自然概率捕获和战斗推进；不写数据库、奖励、经验或随机种子。 */
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
		String recovery = location.startsWith("UNDINE_") || location.startsWith("U_") ? "UNDINE_CITY"
				: location.startsWith("SYLPH_") || location.startsWith("S_") ? "SYLPH_CITY"
						: location.startsWith("SALAMANDER_") || location.startsWith("F_") || location.startsWith("J_")
								? "SALAMANDER_CITY" : "CITY_PETS";
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
				if (turn == 0)
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
				boolean usePotion = !warning && potion != null && injured.hp() < injured.stats().health() * 0.6;
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
		accept(owner, "F001");
		walk(owner, field);
		walk(owner, city);
		deliver(owner, "F001");
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
		System.out.printf("NATURAL_GROWTH encounter=%s target=%d fights=%d xp=%d->%d gold=%d%n", encounter, level,
				count, start, queries.find(owner).experience(), queries.find(owner).gold());
		assertTrue(queries.find(owner).level() >= level, "正式刷怪路线超出额外战斗预算：" + encounter);
	}

	/** 联合物资、外围清理、五阶段深渊和四城收尾均通过正式入口。 */
	void completeJoint(long owner) {
		String city = "SALAMANDER_CITY", field = "J_CORE_PERIMETER", entrance = "J_ABYSS_ENTRANCE";
		clearOutgrownEquipment(owner);
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
		completeRareCollection(owner);
	}

	private void completeRareCollection(long owner) {
		String city = "SALAMANDER_CITY";
		accept(owner, "E011");
		walk(owner, "E_RESIDUAL_FIELD");
		walk(owner, city);
		deliver(owner, "E011");
		accept(owner, "E012");
		walk(owner, "CITY_SHOP");
		for (int ball = 0; ball < 20; ball++)
			battles.purchase(owner, "SEAL_V", UUID.randomUUID().toString());
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
		assertEquals(1, quantity(owner, "F_ARMORY_BLUEPRINT"));
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

	/** 捕捉与战败使用正式成本；依公开血量削弱、治疗、换位，不指定随机种子。 */
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
				boolean usePotion = potion != null && injured.hp() < injured.stats().health() * 0.65;
				String healer = usePotion || injured.hp() >= injured.stats().health() * 0.8 ? null
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
						return new BattleStateOrder(unit.id(), potion, injured.id());
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
				System.out.printf("NATURAL_CAPTURE_DEFEAT encounter=%s attempt=%d gold=%d%n", encounter, attempt + 1,
						queries.find(owner).gold());
		}
		assertEquals("WON", battle.status(), "捕捉必须通过真实概率完成：" + battle.log());
		assertTrue(battle.units().stream().anyMatch(u -> u.enemy() && u.captured()),
				"获胜不能代替实际捕获：" + encounter + " " + battle.log());
		System.out.printf("NATURAL_CAPTURE encounter=%s ball=%s gold=%d%n", encounter, ball,
				queries.find(owner).gold());
	}

	private void buyDeficit(long owner, String code, int required) {
		int missing = required - quantity(owner, code);
		if (missing <= 0)
			return;
		lifeShop.buy(owner, new io.github.lishangbu.avalon.world.application.model.LifeShopRequest(
				UUID.randomUUID().toString(), code, missing));
		assertTrue(quantity(owner, code) >= required);
	}

	private void clearOutgrownEquipment(long owner) {
		walk(owner, "WEAPON_SHOP");
		equip(owner);
		int sold = 0;
		for (var item : items.list(owner)) {
			if (item.slot() == null || item.equipped() || item.requiredLevel() > queries.find(owner).level() - 10)
				continue;
			equipmentCommerce.execute(owner,
					new io.github.lishangbu.avalon.inventory.application.model.EquipmentCommerceRequest(
							UUID.randomUUID().toString(), item.sellPrice() > 0 ? "SELL" : "DESTROY", null, item));
			sold++;
		}
		System.out.printf("NATURAL_BAG clearedEquipment=%d remainingStacks=%d gold=%d%n", sold,
				items.list(owner).size(), queries.find(owner).gold());
	}

	private void walk(long owner, String target) {
		String current = queries.find(owner).location();
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
		if (quantity(owner, material) >= required)
			return;
		walk(owner, "CITY_SHOP");
		if (lifeSkillQueries.list(owner).stream().noneMatch(skill -> skill.code().equals(skillCode) && skill.learned()))
			lifeSkills.change(owner,
					new io.github.lishangbu.avalon.world.application.model.LifeSkillRequest(
							UUID.randomUUID().toString(),
							io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill.valueOf(skillCode), true),
					false);
		lifeShop.buy(owner, new io.github.lishangbu.avalon.world.application.model.LifeShopRequest(
				UUID.randomUUID().toString(), toolCode, 5));
		walk(owner, "CITY_PETS");
		battles.recover(owner);
		walk(owner, location);
		var tool = items.list(owner)
			.stream()
			.filter(i -> i.code().equals(toolCode))
			.max(Comparator.comparingInt(i -> i.quantity()))
			.orElseThrow();
		var pet = pets.list(owner).stream().filter(p -> p.partyPosition() == 0).findFirst().orElseThrow();
		var session = gathering.begin(owner, new io.github.lishangbu.avalon.world.application.model.GatheringStart(
				UUID.randomUUID().toString(), node, pet.id(), tool.id(), tool.quantity()));
		for (int second = 0; quantity(owner, material) < required && second < 240; second++) {
			assertEquals("ACTIVE", session.status(), "采集停止，应保留真实资源约束而非补发矿石");
			// The natural journey advances LifeClock by one second per call. Heartbeats
			// still enforce production costs and intervals without wall-clock sleeping.
			session = gathering.heartbeat(owner, new io.github.lishangbu.avalon.world.application.model.GatheringBeat(
					session.id(), session.revision(), true));
		}
		gathering.stop(owner, new io.github.lishangbu.avalon.world.application.model.GatheringStop(
				UUID.randomUUID().toString(), session.id()));
		assertTrue(quantity(owner, material) >= required, "自然计时与随机产出应能在预算内取得交付材料：" + material);
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

	private void accept(long owner, String step) {
		acceptance.accept(owner, step,
				new QuestAcceptRequest(UUID.randomUUID().toString(), queries.find(owner).revision()));
	}

	private void claim(long owner, String step) {
		accept(owner, step);
		deliver(owner, step);
	}

	private void deliver(long owner, String step) {
		var first = quests.interact(owner, step);
		var before = queries.find(owner);
		assertEquals(first, quests.interact(owner, step));
		assertEquals(before.experience(), queries.find(owner).experience());
		assertEquals(before.gold(), queries.find(owner).gold());
		System.out.printf("NATURAL_STORY step=%s level=%d xp=%d gold=%d%n", step, before.level(), before.experience(),
				before.gold());
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
		for (int n = 0; battle.status().equals("ACTIVE") && n < 40; n++) {
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
			System.out.printf("NATURAL_DEFEAT_LOG encounter=%s log=%s%n", encounter, battle.log());
		return battle.status().equals("WON");
	}

}
