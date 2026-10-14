package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.battle.domain.BattleEngineResources;
import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import io.github.lishangbu.avalon.battle.domain.BattleStateUnit;
import io.github.lishangbu.avalon.battle.domain.DungeonBattlePolicy;
import io.github.lishangbu.avalon.battle.domain.DungeonProgressPolicy;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import static org.junit.jupiter.api.Assertions.*;

/** 推荐等级专项：正式七维与首领参数，基础装备，无药品；战间休整按副本允许的恢复窗口计。 */
@RequiredArgsConstructor
class DungeonCombatSamples {

	private final BattleRules rules;

	private final BattleEngine engine;

	private final BattleCatalog battles;

	private final CreatureCatalog creatures;

	private final DungeonCatalog dungeon;

	private final DungeonBattlePolicy policy;

	/** 入本后至开箱的发起者协议：队友同步操作，不把三人的时间相加。 */
	int personalRunCommands() {
		var progress = new DungeonProgressPolicy(dungeon);
		var members = List.of("a", "b", "c");
		var state = progress.create("a", members);
		int commands = 0;
		for (int index = 0; index < dungeon.definition().stages().size(); index++) {
			if (index > 0) {
				int cleared = index;
				int rest = dungeon.definition()
					.restStops()
					.stream()
					.filter(stop -> stop <= cleared)
					.mapToInt(Integer::intValue)
					.max()
					.orElseThrow();
				for (int position = index - 1; position >= rest; position--) {
					int next = position;
					members.forEach(member -> progress.move(state, member, next));
					commands++;
				}
				members.forEach(member -> progress.rest(state, member));
				commands++;
				for (int position = rest + 1; position <= index; position++) {
					int next = position;
					members.forEach(member -> progress.move(state, member, next));
					commands++;
				}
			}
			members.forEach(member -> progress.ready(state, member));
			commands++;
			assertEquals(dungeon.definition().stages().get(index).id(), progress.nextStage(state));
			progress.begin(state, index + 1L);
			commands++;
			assertTrue(progress.resolve(state, index + 1L, "WON"));
		}
		// 最终胜利自动解除入本占用，不再调用已结束副本的 leave；发起者另领取宝箱一次。
		// 开箱资格与实际发放由 DungeonChestTest / DungeonRewardTest 验证。
		return commands + 1;
	}

	double meanRounds(String middle, int level, int samples) {
		int allRounds = 0;
		var professions = List.of("WARRIOR", middle, "PRIEST");
		for (int sample = 0; sample < samples; sample++) {
			int total = 0;
			for (var stage : dungeon.definition().stages()) {
				var party = new ArrayList<BattleStateUnit>();
				for (int index = 0; index < 3; index++) {
					String heroId = "hero-" + index;
					String profession = professions.get(index);
					boolean physical = List.of("WARRIOR", "RANGER").contains(profession);
					party.add(unit(heroId, profession, true, heroId, 0, level,
							Map.of("health", 40, physical ? "physicalAttack" : "magicAttack", 8, "physicalDefense", 6,
									"magicDefense", 4, "speed", 2)));
					party.add(unit(heroId + "-cat", "CLAW_CAT", false, heroId, 0, level, Map.of()));
					party.add(unit(heroId + "-stone", "STONE_GOLEM", false, heroId, 1, level, Map.of()));
				}
				var state = policy.create(stage.id(), party, 13000 + sample);
				engine.planEnemies(state);
				while (state.getStatus().equals("ACTIVE") && state.getRound() <= 60) {
					var active = state.getUnits()
						.stream()
						.filter(u -> !u.isEnemy() && u.alive() && u.active())
						.toList();
					var enemies = state.getUnits().stream().filter(u -> u.isEnemy() && u.alive()).toList();
					var target = enemies.stream()
						.filter(BattleStateUnit::isMechanism)
						.findFirst()
						.orElse(enemies.getFirst());
					var wounded = active.stream()
						.min(Comparator.comparingDouble(u -> (double) u.getHp() / u.getStats().health()))
						.orElseThrow();
					var orders = new ArrayList<BattleStateOrder>();
					for (var actor : active) {
						String action = "ATTACK", recipient = target.getId();
						if (wounded.getHp() * 10 < wounded.getStats().health() * 7) {
							for (String name : List.of("治疗术", "微疗")) {
								if (actor.getSkills().contains(name)
										&& engine.available(actor, battles.skill(name), state.getRound())) {
									action = name;
									recipient = wounded.getId();
									break;
								}
							}
						}
						if (action.equals("ATTACK")) {
							for (String name : actor.getSkills()) {
								var skill = battles.skill(name);
								if (skill.target().equals("ENEMY")
										&& engine.available(actor, skill, state.getRound())) {
									action = name;
									break;
								}
							}
						}
						orders.add(new BattleStateOrder(actor.getId(), action, recipient));
					}
					BattleTestRounds.resolve(engine, state, orders, false, new BattleEngineResources() {
						public int count(BattleStateUnit actor, String code) {
							return 0;
						}

						public boolean room(BattleStateUnit target) {
							return false;
						}

						public void take(BattleStateUnit actor, String code) {
							fail("无药品样本不可消耗物品");
						}

						public void capture(BattleStateUnit target) {
							fail("副本不捕获");
						}
					});
				}
				assertEquals("WON", state.getStatus(), professions + " " + stage.id() + " " + state.getLog());
				total += state.getRound() - 1;
			}
			allRounds += total;
		}
		return (double) allRounds / samples;
	}

	private BattleStateUnit unit(String id, String species, boolean hero, String owner, int position, int level,
			Map<String, Integer> equipment) {
		var unit = new BattleStateUnit();
		unit.setId(id);
		unit.setName(id);
		unit.setSpecies(species);
		unit.setHero(hero);
		unit.setOwnerHero(owner);
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

}
