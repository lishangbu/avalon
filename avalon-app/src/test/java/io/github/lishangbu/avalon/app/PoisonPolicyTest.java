package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleState.Stats;
import io.github.lishangbu.avalon.battle.domain.BattleState.Unit;
import io.github.lishangbu.avalon.battle.domain.PoisonPolicy;
import io.github.lishangbu.avalon.battle.domain.DirectAttackResolver;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("rules")
class PoisonPolicyTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private final BattleCatalog catalog = fixture.battles;

	private final DirectAttackResolver attacks = fixture.attacks;

	@Test
	void poisonWaitsForFutureTicksAndWeakerSourceCannotRefresh() {
		var state = new BattleState();
		var source = unit("source", false);
		var target = unit("target", true);
		state.setUnits(List.of(source, target));
		PoisonPolicy.apply(state, source, target);
		PoisonPolicy.finish(state, catalog);
		assertEquals(300, target.getHp());
		state.setRound(2);
		PoisonPolicy.finish(state, catalog);
		assertEquals(280, target.getHp());
		source.setLevel(1);
		PoisonPolicy.apply(state, source, target);
		assertEquals(4, target.getPoisonUntil());
		target.setPosition(2);
		target.setEnemy(false);
		state.setRound(3);
		PoisonPolicy.finish(state, catalog);
		assertEquals(280, target.getHp(), "替补不结算过去的跳点");
	}

	@Test
	void synchronizeReflectsOnceAndPoisonHealIsPublicEvenAtFullHealth() {
		var state = new BattleState();
		var source = unit("source", false);
		source.setAbility("同步");
		var target = unit("target", true);
		target.setAbility("同步");
		state.setUnits(List.of(source, target));
		PoisonPolicy.apply(state, source, target);
		assertEquals(4, source.getPoisonUntil());
		assertEquals(4, target.getPoisonUntil());
		assertEquals(2, state.getLog().size(), "反馈不能递归");
		target.setAbility("毒疗");
		state.setRound(2);
		PoisonPolicy.finish(state, catalog);
		assertEquals(300, target.getHp());
		assertTrue(target.isObservedPoisonHealing());
		target.setPeriodicResistance(3);
		target.setHp(200);
		state.setRound(3);
		PoisonPolicy.finish(state, catalog);
		assertEquals(200, target.getHp(), "免疫不产生毒疗收益");
	}

	@Test
	void sheerForceRemovesPoisonAndThornsRequireContactEvenThroughShield() {
		var state = new BattleState();
		var source = unit("source", false);
		source.setAbility("强行");
		var target = unit("target", true);
		target.setAbility("铁刺");
		state.setUnits(List.of(source, target));
		attacks.apply(state, source, List.of(target), catalog.skill("毒液"), false, 1, Set.of(), new HashMap<>());
		assertEquals(248, target.getHp());
		assertEquals(0, target.getPoisonUntil());
		assertEquals(300, source.getHp(), "毒液为非接触");
		target.setShield(100);
		target.setMp(40);
		var attack = new BattleCatalog.Skill("普攻", "DAMAGE", "无", "PA", "PD", 50, 0, 0, 0, "ENEMY");
		attacks.apply(state, source, List.of(target), attack, false, 1, Set.of(), new HashMap<>());
		assertEquals(248, target.getHp());
		assertEquals(285, source.getHp(), "铁刺护盾全吸收也触发，等级5上限15");
	}

	private Unit unit(String id, boolean enemy) {
		var unit = new Unit();
		unit.setId(id);
		unit.setName(id);
		unit.setEnemy(enemy);
		unit.setElement("无");
		unit.setLevel(5);
		unit.setHp(300);
		unit.setStats(new Stats(300, 40, 100, 100, 100, 100, 50));
		return unit;
	}

}
