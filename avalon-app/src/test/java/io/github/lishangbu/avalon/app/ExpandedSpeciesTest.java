package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import io.github.lishangbu.avalon.battle.domain.BattleStateStats;
import io.github.lishangbu.avalon.battle.domain.BattleStateUnit;
import io.github.lishangbu.avalon.battle.domain.DirectAttackResolver;
import io.github.lishangbu.avalon.battle.domain.WildActionPolicy;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.creature.LearningCatalog;
import io.github.lishangbu.avalon.content.creature.model.LearningBook;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 八物种扩展使用确定边界验证成长、条件威力及特性时点，不靠随机采样判断倍率。 */
@Tag("rules")
class ExpandedSpeciesTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private final BattleRules rules = fixture.rules;

	private final BattleCatalog catalog = fixture.battles;

	private final CreatureCatalog creatures = fixture.creatures;

	private final LearningCatalog learning = fixture.learning;

	private final DirectAttackResolver attacks = fixture.attacks;

	private final BattleEngine engine = fixture.engine;

	private final WildActionPolicy wildActions = fixture.wildActions;

	private BattleStateUnit unit(String id, String element, String ability) {
		var unit = new BattleStateUnit();
		unit.setId(id);
		unit.setName(id);
		unit.setLevel(10);
		unit.setElement(element);
		unit.setAbility(ability);
		unit.setStats(new BattleStateStats(1000, 200, 100, 100, 100, 100, 50));
		unit.setHp(1000);
		unit.setMp(200);
		return unit;
	}

	@Test
	void growthAndPoolsMatchAllEightSpecies() {
		assertEquals(new BattleStateStats(378, 96, 42, 63, 56, 63, 52),
				rules.stats("GALE_SPARROW", 10, false, Map.of()));
		assertEquals(new BattleStateStats(360, 128, 42, 88, 49, 63, 44),
				rules.stats("FLAME_RABBIT", 10, false, Map.of()));
		assertEquals(new BattleStateStats(360, 128, 42, 81, 52, 70, 48),
				rules.stats("SHADOWTAIL_FOX", 10, false, Map.of()));
		assertEquals(new BattleStateStats(382, 112, 35, 71, 52, 87, 40),
				rules.stats("MOONLIGHT_MOTH", 10, false, Map.of()));
		for (var species : List.of("CLAW_CAT", "STONE_GOLEM", "SPROUT", "THUNDER_LIZARD", "GALE_SPARROW",
				"FLAME_RABBIT", "SHADOWTAIL_FOX", "MOONLIGHT_MOTH")) {
			var pool = learning.pool(species).stream().map(LearningBook::skill).toList();
			assertEquals(4, pool.size());
			assertTrue(pool.containsAll(creatures.pet(species).skills()));
			assertEquals(creatures.pet(species).skills(), catalog.growth(species).skills());
		}
		assertFalse(learning.pool("SHADOWTAIL_FOX").stream().anyMatch(book -> book.skill().equals("逆风")));
	}

	@Test
	void flamePowerChecksTargetAndOwnHealthSeparatelyWithoutNormalHiddenStacking() {
		var rabbit = unit("rabbit", "火", "聚炎");
		var target = unit("target", "无", null);
		target.setHp(501);
		var burst = catalog.skill("烈焰冲击");
		assertEquals(189, rules.damage(rabbit, target, burst, false, false, 1, 1));
		target.setHp(500);
		assertEquals(283, rules.damage(rabbit, target, burst, false, false, 1, 1));
		rabbit.setAbility("猛火");
		rabbit.setHp(334);
		assertEquals(270, rules.damage(rabbit, target, burst, false, false, 1, 1));
		rabbit.setHp(333);
		assertEquals(310, rules.damage(rabbit, target, burst, false, false, 1, 1));
		// 回血跨过阈值立即取消条件，不保存提交时或上一次攻击的增伤标记。
		target.setHp(501);
		rabbit.setHp(1000);
		assertEquals(180, rules.damage(rabbit, target, burst, false, false, 1, 1));
	}

	@Test
	void firstHeadwindDoesNotBoostItselfAndExpiredSlowDoesNotEnableHiddenAbilities() {
		var bird = unit("bird", "风", "乘风");
		var target = unit("target", "无", null);
		target.setEnemy(true);
		var state = new BattleState();
		state.getUnits().addAll(List.of(bird, target));
		attacks.apply(state, bird, List.of(target), catalog.skill("逆风"), false, 1, Set.of(), Map.of());
		assertEquals(928, target.getHp());
		assertEquals(3, target.getSlowUntil());
		assertEquals(206, rules.damage(bird, target, catalog.skill("风刃"), false, false, 1, 3));
		assertEquals(180, rules.damage(bird, target, catalog.skill("风刃"), false, false, 1, 4));
		var fox = unit("fox", "暗", "夜袭");
		assertEquals(206, rules.damage(fox, target, catalog.skill("暗影箭"), false, false, 1, 3));
		assertEquals(180, rules.damage(fox, target, catalog.skill("暗影箭"), false, false, 1, 4));
		assertEquals(150, rules.damage(fox, target, catalog.skill("火球术"), false, false, 1, 3));
	}

	@Test
	void clearMindRequiresFullHealthAndMagicAndDoesNotInheritVitality() {
		var attacker = unit("attacker", "无", null);
		var moth = unit("moth", "无", "明净");
		assertEquals(127, rules.damage(attacker, moth, catalog.skill("火球术"), false, false, 1, 1));
		assertEquals(120, rules.damage(attacker, moth, catalog.skill("英勇打击"), false, false, 1, 1));
		moth.setHp(999);
		assertEquals(150, rules.damage(attacker, moth, catalog.skill("火球术"), false, false, 1, 1));
		moth.setHp(1000);
		moth.setMagicGuard(true);
		assertEquals(108, rules.damage(attacker, moth, catalog.skill("火球术"), false, false, 1, 1));
	}

	@Test
	void lightnessChangesActualOrderAndHeadwindDoesNotReorderCurrentRound() {
		var bird = unit("bird", "风", "轻盈");
		bird.setStats(new BattleStateStats(1000, 200, 100, 100, 100, 100, 52));
		bird.setSkills(List.of("风刃", "逆风"));
		var enemy = unit("enemy", "无", null);
		enemy.setEnemy(true);
		enemy.setStats(new BattleStateStats(1000, 200, 100, 100, 100, 100, 55));
		var state = new BattleState();
		state.setEncounter("VILLAGE");
		state.getUnits().addAll(List.of(bird, enemy));
		assertEquals(57, rules.speed(bird, 1));
		state.setEnemyOrders(List.of(new BattleStateOrder("enemy", "ATTACK", "bird")));
		engine.resolve(state, List.of(new BattleStateOrder("bird", "逆风", "enemy")), false,
				new BattleRulesTest.FakeResources());
		assertTrue(state.getLog().get(1).startsWith("bird使用逆风"));
		assertEquals(190, bird.getMp());
		assertFalse(engine.available(bird, catalog.skill("逆风"), 2));
		assertFalse(engine.available(bird, catalog.skill("逆风"), 3));
		assertTrue(engine.available(bird, catalog.skill("逆风"), 4));
		assertEquals(44, rules.speed(enemy, 2));
		assertEquals(55, rules.speed(enemy, 4));

		bird.setAbility("乘风");
		assertEquals(52, rules.speed(bird, 4));
		state.setRound(4);
		state.getLog().clear();
		state.setEnemyOrders(List.of(new BattleStateOrder("enemy", "ATTACK", "bird")));
		engine.resolve(state, List.of(new BattleStateOrder("bird", "逆风", "enemy")), false,
				new BattleRulesTest.FakeResources());
		assertTrue(state.getLog().get(1).startsWith("enemy使用普攻"));
	}

	@Test
	void wildPolicyChecksPriorityCooldownAndManaBeforeChoosing() {
		var actor = unit("wild", "风", null);
		actor.setEnemy(true);
		actor.setSkills(List.of("火球术", "逆风", "烈焰冲击", "掩护", "微疗"));
		var friend = unit("friend", "地", null);
		friend.setEnemy(true);
		friend.setHp(350);
		var target = unit("target", "无", null);
		target.setHp(500);
		var state = new BattleState();
		state.getUnits().addAll(List.of(actor, friend, target));
		actor.setHp(500);
		assertEquals(new BattleStateOrder("wild", "微疗", "wild"), wildActions.choose(state, actor, target));
		actor.setHp(501);
		assertEquals(new BattleStateOrder("wild", "掩护", "friend"), wildActions.choose(state, actor, target));
		friend.setHp(351);
		assertEquals(new BattleStateOrder("wild", "逆风", "target"), wildActions.choose(state, actor, target));
		target.setSlowUntil(3);
		assertEquals(new BattleStateOrder("wild", "烈焰冲击", "target"), wildActions.choose(state, actor, target));
		actor.getReady().put("烈焰冲击", 4);
		assertEquals(new BattleStateOrder("wild", "火球术", "target"), wildActions.choose(state, actor, target));
		actor.setMp(7);
		assertEquals(new BattleStateOrder("wild", "ATTACK", "target"), wildActions.choose(state, actor, target));
		// 规划只读战局，不能提前扣资源、附加减速或推进冷却。
		assertEquals(7, actor.getMp());
		assertEquals(3, target.getSlowUntil());
		state.setRound(4);
		actor.setMp(200);
		assertEquals(new BattleStateOrder("wild", "逆风", "target"), wildActions.choose(state, actor, target));
	}

}
