package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.CaveBossPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 验证预告窗口、锁定目标及阶段切换，避免把普通循环误实现为每回合直接重击。 */
class CaveBossPolicyTest {

	private final CaveBossPolicy policy = new CaveBossPolicy();

	@Test
	void pendingHeavySurvivesHalfHealthAndSecondPhaseOpensWithQuake() {
		var state = new BattleState();
		var hero = unit("hero", true, false, 200);
		var pet = unit("pet", false, false, 100);
		var boss = unit("boss", false, true, 1500);
		state.setUnits(List.of(hero, pet, boss));
		execute(state, boss, "BOSS_ATTACK");
		execute(state, boss, "WARN_HEAVY");
		assertEquals("pet", state.getMarkedTarget());
		pet.setHp(300);
		boss.setHp(700);
		assertEquals("pet", policy.plan(state, boss).target(), "治疗不会改选已经点名的个体");
		assertEquals(1, state.getBossPhase());
		execute(state, boss, "BOSS_HEAVY");
		assertEquals(2, state.getBossPhase());
		execute(state, boss, "WARN_QUAKE");
		execute(state, boss, "BOSS_QUAKE");
		execute(state, boss, "BOSS_ATTACK");
		execute(state, boss, "WARN_HEAVY");
		assertEquals("hero", state.getMarkedTarget());
		hero.setHp(0);
		assertEquals("pet", policy.plan(state, boss).target(), "提交前目标倒下时公开改选");
	}

	private void execute(BattleState state, BattleState.Unit boss, String expected) {
		var order = policy.plan(state, boss);
		assertEquals(expected, order.action());
		state.setEnemyOrders(List.of(order));
		if (expected.startsWith("WARN_"))
			policy.warn(state, expected);
		policy.advance(state, boss, expected);
	}

	private BattleState.Unit unit(String id, boolean hero, boolean enemy, int hp) {
		var unit = new BattleState.Unit();
		unit.setId(id);
		unit.setName(id);
		unit.setHero(hero);
		unit.setEnemy(enemy);
		unit.setHp(hp);
		unit.setStats(new BattleState.Stats(enemy ? 1500 : 300, 50, 50, 50, 60, 60, 20));
		return unit;
	}

}
