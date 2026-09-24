package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleState.Order;
import io.github.lishangbu.avalon.battle.domain.BattleState.Unit;
import io.github.lishangbu.avalon.battle.domain.WildGeneration;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.battle.WildCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.inventory.domain.EquipmentLoadoutRules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

/** 正式引擎与目录的逐级样本；固定普通特性、15级副本装备，不预支高阶制造或隐藏特性。 */
@Tag("simulation")
class HighWildBalanceTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private static final int SAMPLES = 256;

	private final BattleEngine engine = fixture.engine;

	private final BattleRules rules = fixture.rules;

	private final WildGeneration generation = fixture.generation;

	private final BattleCatalog battles = fixture.battles;

	private final WildCatalog wilds = fixture.wilds;

	private final CreatureCatalog creatures = fixture.creatures;

	private final ItemCatalog items = fixture.items;

	private final EquipmentLoadoutRules loadouts = fixture.loadouts;

	private final GrowthRules growth = fixture.growth;

	private final ObjectMapper json = new ObjectMapper();

	private final WorldCatalog world = fixture.world;

	private final QuestCatalog quests = fixture.quests;

	@Test
	void continuousFightsRetainResourcesAndHaveAReachableRecoveryRoute() throws Exception {
		var facts = new HashSet<String>();
		quests.definition().steps().forEach(step -> {
			facts.add("QUEST:" + step.id());
			facts.addAll(step.facts());
		});
		var rows = new ArrayList<Endurance>();
		for (String profession : List.of("WARRIOR", "RANGER", "MAGE", "PRIEST")) {
			for (int level : List.of(16, 20, 21, 25, 26, 30, 31, 35, 36, 40, 41, 45, 46, 50)) {
				String region = level <= 20 ? "COPPER" : level <= 30 ? "IRON" : level <= 40 ? "SILVER" : "GOLD";
				if (level > 20 && (level - 1) % 10 >= 5)
					region += "_DEEP";
				var encounter = battles.encounter("HIGH_" + region + "_WILD");
				var outward = recoveryRoute(encounter.location(), facts);
				var returnRoute = route(outward.getLast(), encounter.location(), facts);
				int recoveries = 0, losses = 0, rounds = 0, streak = 0, longest = 0;
				List<Unit> previous = List.of();
				for (int fight = 0; fight < SAMPLES; fight++) {
					// 只根据已公开资源决定恢复，不读取下一场种子或野怪。
					boolean recover = previous.stream()
						.anyMatch(unit -> unit.getHp() * 100 < unit.getStats().health() * 70
								|| unit.getMp() * 100 < unit.getStats().mana() * 25);
					if (recover) {
						recoveries++;
						streak = 0;
					}
					var state = new BattleState();
					state.setEncounter(encounter.id());
					state.setSeed(80000L + fight);
					var party = party(profession, level);
					if (!previous.isEmpty() && !recover)
						for (int index = 0; index < party.size(); index++) {
							party.get(index).setHp(previous.get(index).getHp());
							party.get(index).setMp(previous.get(index).getMp());
						}
					state.getUnits().addAll(party);
					state.getUnits().addAll(generation.create(encounter, new Random(state.getSeed())));
					engine.planEnemies(state);
					while (state.getStatus().equals("ACTIVE") && state.getRound() <= 60)
						engine.resolve(state, orders(state), false, NO_ITEMS);
					if (!state.getStatus().equals("WON"))
						losses++;
					rounds += state.getRound() - 1;
					longest = Math.max(longest, ++streak);
					previous = party;
				}
				rows.add(new Endurance(profession, level, SAMPLES, losses, recoveries, longest,
						(double) rounds / SAMPLES, outward, returnRoute,
						(long) recoveries * (outward.size() + returnRoute.size() - 2)));
			}
		}
		Files.writeString(Path.of("target", "high-wild-endurance.json"),
				json.writerWithDefaultPrettyPrinter().writeValueAsString(rows));
		assertTrue(rows.stream().allMatch(row -> (double) row.losses() / row.battles() <= .05), () -> "连续无药胜率低于95%："
				+ rows.stream().filter(row -> (double) row.losses() / row.battles() > .05).toList());
	}

	private List<Unit> party(String profession, int level) {
		String weapon = Map.of("WARRIOR", "SWORD", "RANGER", "BOW", "MAGE", "STAFF", "PRIEST", "WAND").get(profession);
		var pieces = new ArrayList<EquipmentLoadoutRules.Piece>();
		for (String slot : List.of("HEAD", "BODY", "HANDS", "LEGS", "FEET", "ACCESSORY", weapon)) {
			var equipment = items.equipment("CRYSTAL_" + slot);
			pieces.add(new EquipmentLoadoutRules.Piece(equipment.id(), true, 1, equipment.stats()));
		}
		var loadout = loadouts.from(pieces, level);
		var hero = unit("hero", profession, true, level, 0, loadout.stats());
		hero.setDefendShield(loadout.defendShield());
		return List.of(hero, unit("cat", "CLAW_CAT", false, level - 2, 0, Map.of()),
				unit("sprout", "SPROUT", false, level - 2, 1, Map.of()));
	}

	private List<String> recoveryRoute(String start, HashSet<String> facts) {
		return route(start, null, facts);
	}

	private List<String> route(String start, String target, HashSet<String> facts) {
		var pending = new ArrayDeque<List<String>>();
		var seen = new HashSet<String>();
		pending.add(List.of(start));
		while (!pending.isEmpty()) {
			var path = pending.remove();
			var place = world.place(path.getLast());
			if (!world.accessible(place, facts::contains) || !seen.add(place.id()))
				continue;
			if (target == null ? world.provides(place.id(), "RECOVERY") : place.id().equals(target))
				return path;
			for (String exit : place.exits()) {
				var next = new ArrayList<>(path);
				next.add(exit);
				pending.add(next);
			}
		}
		throw new AssertionError("没有可达恢复往返路线：" + start + " -> " + target);
	}

	record Endurance(String profession, int level, int battles, int losses, int recoveries, int longestStreak,
			double meanRounds, List<String> recoveryRoute, List<String> returnRoute, long travelSteps) {
	}

	@Test
	void everyLevelAndProfessionCanFightWithExistingEquipmentAndEarnProgress() throws Exception {
		var rows = new ArrayList<Sample>();
		for (String profession : List.of("WARRIOR", "RANGER", "MAGE", "PRIEST")) {
			for (int level = 16; level <= GrowthRules.LEVEL_CAP; level++) {
				String region = level <= 20 ? "COPPER" : level <= 30 ? "IRON" : level <= 40 ? "SILVER" : "GOLD";
				if (level > 20 && (level - 1) % 10 >= 5)
					region += "_DEEP";
				var encounter = battles.encounter("HIGH_" + region + "_WILD");
				int experience = 0, rounds = 0, maxRounds = 0, losses = 0, healthSpent = 0, manaSpent = 0;
				for (int seed = 0; seed < SAMPLES; seed++) {
					var state = new BattleState();
					state.setEncounter(encounter.id());
					state.setSeed(50000L + seed);
					state.getUnits().addAll(party(profession, level));
					var enemies = generation.create(encounter, new Random(50000L + seed));
					state.getUnits().addAll(enemies);
					engine.planEnemies(state);
					while (state.getStatus().equals("ACTIVE") && state.getRound() <= 60)
						engine.resolve(state, orders(state), false, NO_ITEMS);
					if (!state.getStatus().equals("WON"))
						losses++;
					int fought = state.getRound() - 1;
					rounds += fought;
					maxRounds = Math.max(maxRounds, fought);
					int multiplier = wilds.profile(encounter.wild()).experienceMultiplier();
					for (var enemy : enemies)
						if (!enemy.alive())
							experience += rules.experience(enemy.getLevel(), level, multiplier);
					for (var member : state.getUnits())
						if (!member.isEnemy()) {
							healthSpent += member.getStats().health() - member.getHp();
							manaSpent += member.getStats().mana() - member.getMp();
						}
				}
				assertTrue(experience > 0);
				long remaining = level == GrowthRules.LEVEL_CAP ? 0
						: growth.threshold(level + 1) - growth.threshold(level);
				rows.add(new Sample(profession, level, SAMPLES, losses, (double) rounds / SAMPLES, maxRounds,
						(double) experience / SAMPLES, (double) healthSpent / SAMPLES, (double) manaSpent / SAMPLES,
						(long) Math.ceil(remaining / ((double) experience / SAMPLES))));
			}
		}
		Path report = Path.of("target", "high-wild-balance.json");
		Files.createDirectories(report.getParent());
		var totals = List.of("WARRIOR", "RANGER", "MAGE", "PRIEST").stream().map(profession -> {
			var samples = rows.stream().filter(row -> row.profession().equals(profession)).toList();
			long battles = samples.stream().mapToLong(Sample::expectedBattlesToNextLevel).sum();
			double rounds = samples.stream()
				.mapToDouble(row -> row.expectedBattlesToNextLevel() * row.meanRounds())
				.sum();
			return new Total(profession, samples.stream().mapToInt(Sample::samples).sum(),
					samples.stream().mapToInt(Sample::losses).sum(),
					samples.stream().mapToDouble(row -> 1 - (double) row.losses() / row.samples()).min().orElseThrow(),
					battles, Math.round(rounds), rounds * 5 / 3600, rounds * 10 / 3600);
		}).toList();
		Files.writeString(report, json.writerWithDefaultPrettyPrinter()
			.writeValueAsString(new Report(
					"固定种子真实引擎；四职业16～50级，每级256场。15级副本装备，低两级普通利爪猫与芽灵，每场满资源、无药。场数为正式经验表与样本均值估算，战败零经验；每回合5/10秒仅是假设，未计恢复、赶路和网络，不是真人耗时。",
					totals, rows)));
		// 这是无药、低两级伙伴、旧装备的基准；保留真实战败，不将所有职业永不战败当成玩法目标。
		assertTrue(rows.stream().allMatch(row -> (double) row.losses() / row.samples() <= .05), () -> "无药胜率低于95%的档位："
				+ rows.stream().filter(row -> (double) row.losses() / row.samples() > .05).toList());
	}

	private Unit unit(String id, String species, boolean hero, int level, int position,
			Map<String, Integer> equipment) {
		var unit = new Unit();
		unit.setId(id);
		unit.setName(id);
		unit.setSpecies(species);
		unit.setHero(hero);
		unit.setOwnerHero("hero");
		unit.setPosition(position);
		unit.setLevel(level);
		unit.setStats(rules.stats(species, level, hero, equipment));
		unit.setHp(unit.getStats().health());
		unit.setMp(unit.getStats().mana());
		unit.setSkills(battles.growth(species).skills());
		if (!hero) {
			unit.setElement(creatures.pet(species).element());
			unit.setAbility(creatures.pet(species).ability());
		}
		return unit;
	}

	private List<Order> orders(BattleState state) {
		var active = state.getUnits()
			.stream()
			.filter(unit -> !unit.isEnemy() && unit.alive() && unit.active())
			.toList();
		var target = state.getUnits().stream().filter(unit -> unit.isEnemy() && unit.alive()).findFirst().orElseThrow();
		var wounded = active.stream()
			.min(Comparator.comparingDouble(unit -> (double) unit.getHp() / unit.getStats().health()))
			.orElseThrow();
		var orders = new ArrayList<Order>();
		for (var unit : active) {
			String action = "ATTACK", recipient = target.getId();
			var attack = new BattleCatalog.Skill("普攻", "DAMAGE", "无", "PA", "PD", 50, 0, 0, 0, "ENEMY");
			int bestDamage = rules.damage(unit, target, attack, false, false, 1, state.getRound());
			for (String name : unit.getSkills()) {
				var skill = battles.skill(name);
				if (!engine.available(unit, skill, state.getRound()))
					continue;
				if (List.of("治疗术", "微疗").contains(name) && wounded.getHp() * 10 < wounded.getStats().health() * 7) {
					action = name;
					recipient = wounded.getId();
					break;
				}
				if (skill.target().equals("ENEMY")) {
					int damage = rules.damage(unit, target, skill, false, false, 1, state.getRound());
					if (damage > bestDamage) {
						bestDamage = damage;
						action = name;
					}
				}
			}
			orders.add(new Order(unit.getId(), action, recipient));
		}
		return orders;
	}

	private static final BattleEngine.Resources NO_ITEMS = new BattleEngine.Resources() {
		public int count(String code) {
			return 0;
		}

		public boolean room(Unit target) {
			return false;
		}

		public void take(String code) {
			fail("无药品样本不可消耗物品");
		}

		public void capture(Unit target) {
			fail("战斗样本不捕获");
		}
	};

	record Sample(String profession, int level, int samples, int losses, double meanRounds, int maxRounds,
			double meanExperience, double meanHealthSpent, double meanManaSpent, long expectedBattlesToNextLevel) {
	}

	record Total(String profession, int samples, int losses, double minimumWinRate, long expectedBattles16To50,
			long expectedRounds, double hoursAtFiveSecondsPerRound, double hoursAtTenSecondsPerRound) {
	}

	record Report(String scope, List<Total> totals, List<Sample> rows) {
	}

}
