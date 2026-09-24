package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleState.*;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** 正式遭遇而非训练模式下验证职业技能的持续期、快照、归属与完整回合冷却。 */
@Tag("rules")
class ProfessionSkillRulesTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private final BattleEngine engine = fixture.engine;

	private final BattleRules rules = fixture.rules;

	private final BattleCatalog catalog = fixture.battles;

	private static final BattleEngine.Resources NO_ITEMS = new BattleEngine.Resources() {
		public int count(String code) {
			return 0;
		}

		public boolean room(Unit target) {
			return false;
		}

		public void take(String code) {
			fail("不可消耗道具");
		}

		public void capture(Unit target) {
			fail("不可捕捉");
		}
	};

	private Unit unit(String id, boolean enemy, boolean hero) {
		var unit = new Unit();
		unit.setId(id);
		unit.setName(id);
		unit.setSpecies("CLAW_CAT");
		unit.setEnemy(enemy);
		unit.setHero(hero);
		unit.setLevel(10);
		unit.setStats(new Stats(1000, 200, 100, 100, 100, 100, 50));
		unit.setHp(1000);
		unit.setMp(200);
		return unit;
	}

	private BattleState state(Unit... units) {
		var state = new BattleState();
		state.setEncounter("VILLAGE");
		state.setSeed(321);
		state.getUnits().addAll(List.of(units));
		return state;
	}

	private void resolve(BattleState state, Order... orders) {
		// 清空敌方动作仅隔离本次被测效果；所有玩家行动仍完整校验、消耗并结算。
		state.setEnemyOrders(List.of());
		engine.resolve(state, List.of(orders), false, NO_ITEMS);
	}

	@Test
	void wallLastsTwoRoundsAndMarkOnlyBenefitsOwnerAndOwnedPets() {
		var hero = unit("hero", false, true);
		var enemy = unit("enemy", true, false);
		hero.setSkills(List.of("盾墙"));
		var state = state(hero, enemy);
		resolve(state, new Order("hero", "盾墙", "hero"));
		var attack = engine.skill("ATTACK");
		assertEquals(45, rules.damage(enemy, hero, attack, false, false, 1, 1));
		assertEquals(45, rules.damage(enemy, hero, attack, false, false, 1, 2));
		assertEquals(75, rules.damage(enemy, hero, attack, false, false, 1, 3));
		assertEquals(188, hero.getMp());
		assertFalse(engine.available(hero, catalog.skill("盾墙"), 5));
		assertTrue(engine.available(hero, catalog.skill("盾墙"), 6));
		hero.setSkills(List.of("猎人印记"));
		resolve(state, new Order("hero", "猎人印记", "enemy"));
		var pet = unit("pet", false, false);
		pet.setOwnerHero("hero");
		var stranger = unit("other", false, true);
		for (var actor : List.of(hero, pet)) {
			assertEquals(82, rules.damage(actor, enemy, attack, false, false, 1, 4));
			assertEquals(75, rules.damage(actor, enemy, attack, false, false, 1, 5));
		}
		assertEquals(75, rules.damage(stranger, enemy, attack, false, false, 1, 3));
		pet.setOwnerHero("other");
		assertEquals(75, rules.damage(pet, enemy, attack, false, false, 1, 3));
	}

	@Test
	void renewSnapshotsThreeEndRoundTicksAndCannotResurrect() {
		var hero = unit("hero", false, true);
		var pet = unit("pet", false, false);
		var enemy = unit("enemy", true, false);
		hero.setSkills(List.of("恢复"));
		pet.setHp(100);
		var state = state(hero, pet, enemy);
		resolve(state, new Order("hero", "恢复", "pet"), new Order("pet", "DEFEND", "pet"));
		assertEquals(160, pet.getHp());
		hero.setStats(new Stats(1000, 200, 100, 200, 100, 100, 50));
		for (int i = 0; i < 2; i++)
			resolve(state, new Order("hero", "DEFEND", "hero"), new Order("pet", "DEFEND", "pet"));
		assertEquals(280, pet.getHp());
		resolve(state, new Order("hero", "DEFEND", "hero"), new Order("pet", "DEFEND", "pet"));
		assertEquals(280, pet.getHp());
		pet.setHp(999);
		resolve(state, new Order("hero", "恢复", "pet"), new Order("pet", "DEFEND", "pet"));
		assertEquals(1000, pet.getHp());
		pet.setHp(0);
		resolve(state, new Order("hero", "DEFEND", "hero"));
		assertEquals(0, pet.getHp());
		assertEquals(0, pet.getRenewUntil());
	}

	@Test
	void areaUsesExplicitDistinctTargetsOneCostAndSharedRandomWithoutSplitting() {
		var hero = unit("hero", false, true);
		hero.setSkills(List.of("魔爆术"));
		var a = unit("a", true, false);
		var b = unit("b", true, false);
		var c = unit("c", true, false);
		var state = state(hero, a, b, c);
		assertThrows(BusinessException.class, () -> resolve(state, new Order("hero", "魔爆术", null, List.of("a", "a"))));
		assertThrows(BusinessException.class, () -> resolve(state, new Order("hero", "魔爆术", null, List.of("hero"))));
		resolve(state, new Order("hero", "魔爆术", null, List.of("a", "b")));
		assertEquals(186, hero.getMp());
		assertEquals(a.getHp(), b.getHp());
		assertTrue(a.getHp() >= 921 && a.getHp() <= 930);
		assertEquals(1000, c.getHp());
		assertFalse(engine.available(hero, catalog.skill("魔爆术"), 2));
		assertTrue(engine.available(hero, catalog.skill("魔爆术"), 3));
	}

	/** 新进化技能走正式回合管线，不能只证明目录中存在名称。此夹具不作为20级实战平衡数据。 */
	@Test
	void evolvedSkillsConsumeOnceRespectFullCooldownAndOnlyHitSelectedEnemies() {
		var falcon = unit("falcon", false, false);
		falcon.setSpecies("GALE_FALCON");
		falcon.setElement("风");
		falcon.setSkills(List.of("疾风刃"));
		var target = unit("target", true, false);
		var single = state(falcon, target);
		resolve(single, new Order("falcon", "疾风刃", "target"));
		assertEquals(182, falcon.getMp());
		assertTrue(target.getHp() < 1000);
		assertFalse(engine.available(falcon, catalog.skill("疾风刃"), 2));
		assertTrue(engine.available(falcon, catalog.skill("疾风刃"), 3));

		var rabbit = unit("rabbit", false, false);
		rabbit.setSpecies("FLAME_SPIRIT_RABBIT");
		rabbit.setElement("火");
		rabbit.setSkills(List.of("烈焰风暴"));
		var a = unit("a", true, false);
		var b = unit("b", true, false);
		var c = unit("c", true, false);
		var d = unit("d", true, false);
		var area = state(rabbit, a, b, c, d);
		assertThrows(BusinessException.class,
				() -> resolve(area, new Order("rabbit", "烈焰风暴", null, List.of("a", "b", "c", "d"))));
		assertEquals(200, rabbit.getMp());
		resolve(area, new Order("rabbit", "烈焰风暴", null, List.of("a", "b", "c")));
		assertEquals(176, rabbit.getMp());
		assertTrue(a.getHp() < 1000);
		assertEquals(a.getHp(), b.getHp());
		assertEquals(a.getHp(), c.getHp());
		assertEquals(1000, d.getHp());
		assertFalse(engine.available(rabbit, catalog.skill("烈焰风暴"), 2));
		assertFalse(engine.available(rabbit, catalog.skill("烈焰风暴"), 3));
		assertTrue(engine.available(rabbit, catalog.skill("烈焰风暴"), 4));
	}

}
