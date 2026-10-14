package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.battle.application.model.BattleView;
import io.github.lishangbu.avalon.battle.application.model.BattleViewAction;
import io.github.lishangbu.avalon.battle.application.model.BattleViewCaptureOption;
import io.github.lishangbu.avalon.battle.application.model.BattleViewUnitView;
import io.github.lishangbu.avalon.battle.application.query.BattleQueryService;
import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import io.github.lishangbu.avalon.battle.domain.BattleStateUnit;
import io.github.lishangbu.avalon.battle.domain.WildGeneration;
import io.github.lishangbu.avalon.battle.persistence.repository.BattleRecordRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.content.battle.WildCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.persistence.EntityManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import me.ahoo.cosid.IdGenerator;
import me.ahoo.cosid.provider.IdGeneratorProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.ObjectMapper;

/** 随机池验证与数据库捕获、结算分开；战局夹具只消除概率及伤害噪声，不替代正式命令。 */
class RegionalEncounterTest extends SpringIntegrationTest {

	@Autowired
	GrowthRules growth;

	@Autowired
	WorldCatalog world;

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	BattleCommandService commands;

	@Autowired
	BattleQueryService queries;

	@Autowired
	BattleCatalog battles;

	@Autowired
	WildCatalog wilds;

	@Autowired
	WildGeneration generation;

	@Autowired
	BattleRules rules;

	@Autowired
	BattleRecordRepository battleRecordRepository;

	@Autowired
	CombatVitalsRepository combatVitalsRepository;

	@Autowired
	CombatVitalsStore vitals;

	@Autowired
	QuestCatalog questCatalog;

	@Autowired
	ObjectMapper codec;

	@Autowired
	CreatureQueryService creatures;

	@Autowired
	CreatureCommandService creatureCommands;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService bag;

	@Autowired
	EntityManager em;

	@Autowired
	@Qualifier(IdGeneratorProvider.SHARE + "SnowflakeId")
	IdGenerator ids;

	private long ready(String location, int level) {
		return ready(location, level, "WARRIOR");
	}

	private long ready(String location, int level, String profession) {
		String name = "wild_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		registrations.register(name, "Region-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, profession);
		transactions.run(() -> {
			characters.grantExperience(owner, Math.toIntExact(growth.threshold(level)));
			long id = characters.lock(owner).id();
			em.createNativeQuery("UPDATE player_character SET location = :location WHERE id = :id")
				.setParameter("location", location)
				.setParameter("id", id)
				.executeUpdate();
			for (String step : List.of("BQ007", "BQ009", "BQ014", "BQ017", "BQ029", "BQ066")) {
				em.createNativeQuery(
						"INSERT INTO quest_receipt(id, character_id, step, quest_code, completed, response, created_at) "
								+ "VALUES(:id, :character, :step, :quest, false, '{}', CURRENT_TIMESTAMP)")
					.setParameter("id", ids.generate())
					.setParameter("character", id)
					.setParameter("step", step)
					.setParameter("quest", step)
					.executeUpdate();
			}
			inventory.grant(owner, "TEST:I", "SEAL_I", 50, false);
			inventory.grant(owner, "TEST:II", "STRONG_SEAL_II", 50, false);
		});
		return owner;
	}

	private void edit(BattleView battle, Consumer<BattleState> operation) {
		transactions.run(() -> {
			var entity = battleRecordRepository.get(Long.parseLong(battle.id()));
			var state = codec.readValue(entity.getState(), BattleState.class);
			operation.accept(state);
			entity.setState(codec.writeValueAsString(state));
		});
	}

	@ParameterizedTest
	@ValueSource(strings = { "WARRIOR", "RANGER", "MAGE", "PRIEST" })
	@EnabledIfEnvironmentVariable(named = "AVALON_HIGH_GROWTH", matches = "1")
	void growsFromSixteenToFiftyThroughPersistedBattles(String profession) throws Exception {
		long owner = ready("COPPER_WETLAND", 16, profession);
		String weapon = Map.of("WARRIOR", "SWORD", "RANGER", "BOW", "MAGE", "STAFF", "PRIEST", "WAND").get(profession);
		transactions.run(() -> {
			questCatalog.definition().steps().forEach(step -> {
				facts.record(owner, "QUEST:" + step.id());
				step.objectives().stream().map(goal -> goal.code()).toList().forEach(fact -> facts.record(owner, fact));
			});
			for (String slot : List.of("HEAD", "BODY", "HANDS", "LEGS", "FEET", "ACCESSORY", weapon))
				inventory.grant(owner, "JOURNEY:" + slot, "CRYSTAL_" + slot, 1, true);
		});
		for (var item : bag.list(owner))
			if (item.code().startsWith("CRYSTAL_"))
				inventory.equip(owner, Long.parseLong(item.id()));
		for (String species : List.of("CLAW_CAT", "SPROUT")) {
			long pet = transactions.call(() -> creatureCommands.capture(owner, "JOURNEY:" + species, species, 14, "A",
					"完好防护", battles.growth(species).skills()));
			creatureCommands.position(owner, pet, species.equals("CLAW_CAT") ? 0 : 1);
		}
		var checkpoints = new ArrayList<Map<String, Object>>();
		int fights = 0, losses = 0, rounds = 0, recoveries = 0, moves = 0;
		boolean recover = false;
		while (characterQueries.find(owner).level() < 50 && fights < 20000) {
			int level = characterQueries.find(owner).level();
			String region = level <= 20 ? "COPPER" : level <= 30 ? "IRON" : level <= 40 ? "SILVER" : "GOLD";
			String encounter = "HIGH_" + region + (level > 20 && (level - 1) % 10 >= 5 ? "_DEEP" : "") + "_WILD";
			if (recover) {
				moves += journeyMove(owner, null);
				commands.recover(owner);
				recoveries++;
			}
			moves += journeyMove(owner, battles.encounter(encounter).location());
			long before = characterQueries.find(owner).experience();
			var battle = commands.start(owner, encounter, UUID.randomUUID().toString());
			while (battle.status().equals("ACTIVE") && battle.round() <= 60) {
				String target = battle.units()
					.stream()
					.filter(unit -> unit.enemy() && unit.hp() > 0)
					.findFirst()
					.orElseThrow()
					.id();
				var orders = battle.units()
					.stream()
					.filter(unit -> !unit.enemy() && unit.hp() > 0 && (unit.hero() || unit.position() < 2))
					.map(unit -> new BattleStateOrder(unit.id(), unit.actions()
						.stream()
						.filter(action -> action.available() && action.mana() > 0 && action.target().equals("ENEMY"))
						.map(BattleViewAction::id)
						.findFirst()
						.orElse("ATTACK"), target))
					.toList();
				battle = BattleTestCommands.turn(commands, owner, Long.parseLong(battle.id()), battle.round(), orders,
						false);
				rounds++;
			}
			assertNotEquals("ACTIVE", battle.status(), "战斗超过60回合");
			fights++;
			if (!battle.status().equals("WON"))
				losses++;
			else
				assertTrue(characterQueries.find(owner).experience() > before, "胜利必须推进人物经验");
			recover = !battle.status().equals("WON") || battle.units()
				.stream()
				.filter(unit -> !unit.enemy() && (unit.hero() || unit.position() < 2))
				.anyMatch(unit -> unit.hp() * 100 < unit.stats().health() * 70
						|| unit.mp() * 100 < unit.stats().mana() * 25);
			if (characterQueries.find(owner).level() != level) {
				checkpoints.add(Map.of("level", characterQueries.find(owner).level(), "battles", fights, "losses",
						losses, "rounds", rounds, "recoveries", recoveries, "moves", moves, "experience",
						characterQueries.find(owner).experience(), "pets", creatures.list(owner)));
				Files.writeString(
						Path.of("target", "high-growth-journey-" + profession.toLowerCase(Locale.ROOT) + ".json"),
						codec.writerWithDefaultPrettyPrinter().writeValueAsString(checkpoints));
				System.out
					.println("成长存档：职业=" + profession + " 等级=" + characterQueries.find(owner).level() + " 战斗=" + fights);
			}
		}
		assertEquals(50, characterQueries.find(owner).level());
		assertEquals(34, checkpoints.size());
		assertTrue(losses * 100 <= fights * 5, "失败比例超过5%");
	}

	private int journeyMove(long owner, String destination) {
		var pending = new ArrayDeque<List<String>>();
		var seen = new HashSet<String>();
		pending.add(List.of(characterQueries.find(owner).location()));
		while (!pending.isEmpty()) {
			var path = pending.remove();
			String place = path.getLast();
			if (!seen.add(place) || !world.accessible(world.place(place), ignored -> true))
				continue;
			if (destination == null ? world.provides(place, "RECOVERY") : place.equals(destination)) {
				for (String step : path.subList(1, path.size()))
					characters.move(owner, step, characterQueries.find(owner).revision());
				return path.size() - 1;
			}
			for (String exit : world.place(place).exits()) {
				var next = new ArrayList<>(path);
				next.add(exit);
				pending.add(next);
			}
		}
		throw new AssertionError("没有可用路线：" + destination);
	}

	@Test
	void allHighRegionsAllowActualRecoveryAndReturnWithoutChargingGold() {
		var routes = List.of(List.of("COPPER_WETLAND", "CITY_OUTSKIRTS", "GNOME_CITY", "CITY_PETS"),
				List.of("IRON_HIGHLAND", "UNDINE_WHARF"), List.of("SILVER_FROSTWOOD", "IRON_HIGHLAND", "UNDINE_WHARF"),
				List.of("GOLD_DAWN_VALLEY", "SILVER_FROSTWOOD", "IRON_HIGHLAND", "UNDINE_WHARF"));
		for (var route : routes) {
			long owner = ready(route.getFirst(), 50);
			long pet = transactions.call(() -> creatureCommands.capture(owner, "RECOVERY_PARTNER", "CLAW_CAT", 44, "A",
					"完好防护", List.of("水弹")));
			long hero = characterQueries.requireId(owner);
			transactions.run(() -> {
				characters.lock(owner);
				questCatalog.definition().steps().forEach(step -> {
					facts.record(owner, "QUEST:" + step.id());
					step.objectives()
						.stream()
						.map(goal -> goal.code())
						.toList()
						.forEach(fact -> facts.record(owner, fact));
				});
				vitals.saveVitals(hero, hero, 1, 0);
				vitals.saveVitals(hero, pet, 0, 0);
			});
			long gold = characterQueries.find(owner).gold();
			assertThrows(BusinessException.class, () -> commands.recover(owner));
			for (String destination : route.subList(1, route.size()))
				characters.move(owner, destination, characterQueries.find(owner).revision());
			commands.recover(owner);
			transactions.run(() -> {
				assertEquals(rules.stats("WARRIOR", 50, true, Map.of()).health(),
						combatVitalsRepository.get(hero).getHp());
				assertEquals(rules.stats("WARRIOR", 50, true, Map.of()).mana(),
						combatVitalsRepository.get(hero).getMp());
				assertEquals(rules.stats("CLAW_CAT", 44, false, Map.of()).health(),
						combatVitalsRepository.get(pet).getHp());
				assertEquals(rules.stats("CLAW_CAT", 44, false, Map.of()).mana(),
						combatVitalsRepository.get(pet).getMp());
			});
			for (int index = route.size() - 2; index >= 0; index--)
				characters.move(owner, route.get(index), characterQueries.find(owner).revision());
			assertEquals(route.getFirst(), characterQueries.find(owner).location());
			assertEquals(gold, characterQueries.find(owner).gold());
		}
	}

	@Test
	void highLevelExperienceUsesItsOwnMultiplierWithoutIncreasingGoldAndReplaysOnce() {
		long owner = ready("GOLD_DAWN_VALLEY", 46);
		long pet = transactions.call(() -> creatureCommands.capture(owner, "EXPERIENCE_PARTNER", "CLAW_CAT", 44, "A",
				"完好防护", List.of("水弹")));
		creatureCommands.position(owner, pet, 2);
		long petBefore = creatures.list(owner).getFirst().experience();
		var battle = commands.start(owner, "HIGH_GOLD_DEEP_WILD", UUID.randomUUID().toString());
		int enemyLevel = battle.units().stream().filter(BattleViewUnitView::enemy).findFirst().orElseThrow().level();
		edit(battle, state -> {
			state.setSeed(3);
			state.setEnemyOrders(List.of());
			state.getUnits().stream().filter(BattleStateUnit::isEnemy).forEach(unit -> unit.setHp(1));
		});
		long before = characterQueries.find(owner).experience();
		long gold = characterQueries.find(owner).gold();
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			assertEquals("WON", action(owner, battle, "ATTACK", "enemy-1").status());
			throw new IllegalStateException("模拟提交失败");
		}));
		assertEquals(before, characterQueries.find(owner).experience());
		assertEquals(petBefore, creatures.list(owner).getFirst().experience());
		assertEquals(gold, characterQueries.find(owner).gold());
		var won = action(owner, battle, "ATTACK", "enemy-1");
		assertEquals("WON", won.status());
		assertEquals(before + rules.experience(enemyLevel, 46, 16), characterQueries.find(owner).experience());
		assertEquals(petBefore + rules.experience(enemyLevel, 44, 16), creatures.list(owner).getFirst().experience());
		assertEquals(gold + (enemyLevel + 2) / 3 * 2, characterQueries.find(owner).gold());
		assertEquals(won, action(owner, battle, "ATTACK", "enemy-1"));
		assertEquals(petBefore + rules.experience(enemyLevel, 44, 16), creatures.list(owner).getFirst().experience());
		assertEquals(before + rules.experience(enemyLevel, 46, 16), characterQueries.find(owner).experience());
		assertEquals(gold + (enemyLevel + 2) / 3 * 2, characterQueries.find(owner).gold());
	}

	@Test
	void highTierPurchaseAndLevelFiftyCapturePersistWithoutDuplicateAssets() {
		long owner = ready("CITY_SHOP", 50);
		transactions.run(() -> {
			characters.changeGold(owner, 1000);
			facts.record(owner, "QUEST:BQ066");
		});
		String purchase = UUID.randomUUID().toString();
		commands.purchase(owner, "STRONG_SEAL_V", purchase);
		commands.purchase(owner, "STRONG_SEAL_V", purchase);
		assertEquals(616, characterQueries.find(owner).gold());
		assertEquals(1,
				bag.list(owner)
					.stream()
					.filter(item -> item.code().equals("STRONG_SEAL_V"))
					.mapToInt(item -> item.quantity())
					.sum());
		transactions.run(() -> {
			em.createNativeQuery("UPDATE player_character SET location='GOLD_DAWN_VALLEY' WHERE id=:id")
				.setParameter("id", characters.lock(owner).id())
				.executeUpdate();
			inventory.grantStrict(owner, "HIGH_CAPTURE_SPARES", "STRONG_SEAL_V", 29, false, false);
		});
		var battle = commands.start(owner, "HIGH_GOLD_DEEP_WILD", UUID.randomUUID().toString());
		// 使用真实野外生成的50级个体，仅固定随机种子和濒死资源来隔离捕获事务。
		edit(battle, state -> {
			BattleStateUnit target = null;
			for (int seed = 0; seed < 1000 && target == null; seed++) {
				var candidate = generation.create(battles.encounter("HIGH_GOLD_DEEP_WILD"), new Random(seed))
					.getFirst();
				if (candidate.getLevel() == 50)
					target = candidate;
			}
			assertNotNull(target);
			target.setHp(1);
			target.setMp(0);
			state.getUnits().removeIf(BattleStateUnit::isEnemy);
			state.getUnits().add(target);
			state.setSeed(12345);
			state.setEnemyOrders(List.of());
		});
		long experience = characterQueries.find(owner).experience();
		for (int attempt = 0; attempt < 30 && battle.status().equals("ACTIVE"); attempt++)
			battle = action(owner, battle, "STRONG_SEAL_V", "enemy-1");
		assertEquals("WON", battle.status());
		var pet = creatures.list(owner).getFirst();
		assertEquals(50, pet.level());
		assertEquals(1, transactions.call(() -> combatVitalsRepository.get(Long.parseLong(pet.id())).getHp()));
		assertEquals(experience, characterQueries.find(owner).experience());
		var storedItems = bag.list(owner);
		var finished = battle;
		assertThrows(BusinessException.class, () -> BattleTestCommands.turn(commands, owner,
				Long.parseLong(finished.id()), finished.round() - 1, List.of(), false));
		assertEquals(storedItems, bag.list(owner));
		assertEquals(List.of(pet), creatures.list(owner));
	}

	private BattleView action(long owner, BattleView battle, String action, String target) {
		String hero = battle.units().stream().filter(BattleViewUnitView::hero).findFirst().orElseThrow().id();
		return BattleTestCommands.turn(commands, owner, Long.parseLong(battle.id()), battle.round(),
				List.of(new BattleStateOrder(hero, action, target)), false);
	}

	@Test
	void regionRangesWeightsAndIndependentDuplicatesAreStable() {
		var counts = new HashMap<String, Integer>();
		boolean duplicate = false;
		for (int seed = 0; seed < 10000; seed++) {
			var units = generation.create(battles.encounter("PLAINS_DUO"), new Random(seed));
			assertEquals(2, units.size());
			assertNotEquals(units.get(0).getId(), units.get(1).getId());
			duplicate |= units.get(0).getSpecies().equals(units.get(1).getSpecies());
			for (var unit : units) {
				var range = wilds.profile("PLAINS").species().get(unit.getSpecies());
				assertTrue(unit.getLevel() >= range.minLevel() && unit.getLevel() <= range.maxLevel());
				assertFalse(unit.isElite());
				counts.merge(unit.getSpecies(), 1, Integer::sum);
			}
		}
		assertTrue(duplicate);
		wilds.profile("PLAINS")
			.species()
			.forEach((code, spawn) -> assertEquals(spawn.weight() / 100.0, counts.get(code) / 20000.0, .025));
		var elite = generation.create(battles.encounter("ELITE_FOX"), new Random(1)).getFirst();
		var normal = rules.stats("SHADOWTAIL_FOX", 13, false, Map.of());
		assertEquals(normal.health() * 3, elite.getStats().health());
		assertEquals((int) Math.floor(normal.magicAttack() * 1.1), elite.getStats().magicAttack());
		assertEquals(normal.magicDefense(), elite.getStats().magicDefense());
		assertEquals(8, generation.create(battles.encounter("SPARROW_TRACK"), new Random(1)).getFirst().getLevel());
	}

	@Test
	void dualCaptureKeepsTwoIndividualsAndReplayDoesNotReroll() {
		long owner = ready("PLAIN_SIGN", 11);
		String request = UUID.randomUUID().toString();
		var battle = commands.start(owner, "PLAINS_DUO", request);
		assertEquals(battle, commands.start(owner, "PLAINS_DUO", request));
		assertEquals(battle, queries.current(owner));
		assertEquals(2, battle.units().stream().filter(BattleViewUnitView::enemy).count());
		edit(battle, state -> {
			state.setSeed(12345);
			for (var unit : state.getUnits())
				if (unit.isEnemy()) {
					unit.setHp(1);
					unit.setMp(0);
					unit.setSkills(List.of());
				}
			state.setEnemyOrders(List.of());
		});
		long before = characterQueries.find(owner).experience();
		for (int i = 0; i < 30 && battle.status().equals("ACTIVE"); i++) {
			String target = battle.units()
				.stream()
				.filter(unit -> unit.enemy() && !unit.captured())
				.findFirst()
				.orElseThrow()
				.id();
			battle = action(owner, battle, "STRONG_SEAL_II", target);
		}
		assertEquals("WON", battle.status());
		assertEquals(2, creatures.list(owner).size(), "同场第二只捕获不得命中第一只的来源凭证");
		assertTrue(battle.units().stream().filter(BattleViewUnitView::enemy).allMatch(BattleViewUnitView::captured));
		assertEquals(before, characterQueries.find(owner).experience(), "捕获没有击败经验");
		var finished = battle;
		assertThrows(BusinessException.class, () -> BattleTestCommands.turn(commands, owner,
				Long.parseLong(finished.id()), finished.round() - 1, List.of(), false));
		assertEquals(2, creatures.list(owner).size());
	}

	@Test
	void eliteGateBallQualificationCaptureNormalizationAndSafeReturn() {
		long low = ready("CITY_OUTSKIRTS", 1);
		assertTrue(queries.lobby(low).encounters().stream().noneMatch(e -> e.id().equals("ELITE_FOX")));
		assertThrows(BusinessException.class, () -> commands.start(low, "ELITE_FOX", UUID.randomUUID().toString()));
		long owner = ready("CITY_OUTSKIRTS", 13);
		var battle = commands.start(owner, "ELITE_FOX", UUID.randomUUID().toString());
		var enemy = battle.units().stream().filter(BattleViewUnitView::enemy).findFirst().orElseThrow();
		assertEquals(List.of("STRONG_SEAL_II"), enemy.captures().stream().map(BattleViewCaptureOption::code).toList());
		assertEquals(enemy.stats().health() / 3, enemy.captureStats().health());
		var original = battle;
		var items = bag.list(owner);
		assertThrows(BusinessException.class, () -> action(owner, original, "SEAL_I", enemy.id()));
		assertEquals(original, queries.current(owner));
		assertEquals(items, bag.list(owner));
		edit(battle, state -> {
			state.setSeed(12345);
			state.getUnits().stream().filter(BattleStateUnit::isEnemy).forEach(unit -> {
				unit.setHp(1);
				unit.setMp(0);
				unit.setSkills(List.of());
			});
			state.setEnemyOrders(List.of());
		});
		for (int i = 0; i < 30 && battle.status().equals("ACTIVE"); i++)
			battle = action(owner, battle, "STRONG_SEAL_II", enemy.id());
		assertEquals("WON", battle.status());
		var pet = creatures.list(owner).getFirst();
		assertEquals(13, pet.level());
		assertEquals("SHADOWTAIL_FOX", pet.species());
		assertEquals(1, transactions.call(() -> combatVitalsRepository.get(Long.parseLong(pet.id())).getHp()));
		var next = commands.start(owner, "ELITE_FOX", UUID.randomUUID().toString());
		assertEquals("ACTIVE", next.status(), "精英不限首通次数");
		BattleTestCommands.turn(commands, owner, Long.parseLong(next.id()), next.round(), List.of(), true);
		assertEquals("SQUARE", characterQueries.find(owner).location(), "仅有任务凭证夹具不伪造已解锁城镇事实");
	}

	@Test
	void defeatedEliteUsesItsOwnRewardBudgetAndCommitRollbackIsAtomic() {
		long owner = ready("FOREST_EDGE", 8);
		var battle = commands.start(owner, "ELITE_GOLEM", UUID.randomUUID().toString());
		edit(battle, state -> {
			state.setSeed(3);
			state.setEnemyOrders(List.of());
			state.getUnits().stream().filter(BattleStateUnit::isEnemy).forEach(unit -> unit.setHp(1));
		});
		long before = characterQueries.find(owner).experience();
		int level = characterQueries.find(owner).level();
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			assertEquals("WON", action(owner, battle, "ATTACK", "enemy-1").status());
			throw new IllegalStateException("模拟提交失败");
		}));
		assertEquals(before, characterQueries.find(owner).experience());
		assertEquals("ACTIVE", queries.current(owner).status());
		var won = action(owner, battle, "ATTACK", "enemy-1");
		assertEquals("WON", won.status());
		assertEquals(before + rules.experience(6, level, 2), characterQueries.find(owner).experience());
		assertEquals(4, characterQueries.find(owner).gold());
		assertTrue(bag.list(owner)
			.stream()
			.noneMatch(item -> item.code().equals("BOOK_ROCK") || item.code().equals("FRAGMENT_ROCK")));
		assertEquals(won, action(owner, battle, "ATTACK", "enemy-1"));
		assertEquals(4, characterQueries.find(owner).gold());
	}

}
