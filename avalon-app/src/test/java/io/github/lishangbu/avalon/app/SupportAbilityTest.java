package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import io.github.lishangbu.avalon.battle.domain.BattleStateStats;
import io.github.lishangbu.avalon.battle.domain.BattleStateUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 通过完整回合验证实际治疗、耗魔及主动换宠，不用直接设置触发后的状态代替行为。 */
@Tag("rules")
class SupportAbilityTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private final BattleEngine engine = fixture.engine;

	@Test
	void communionUsesActualHealingAndNeverHealsFromAnotherOwnersPet() {
		var state = state("生命共鸣");
		var caster = state.getUnits().get(0);
		var target = state.getUnits().get(1);
		target.setHp(280);
		heal(state);
		assertEquals(300, target.getHp());
		assertEquals(105, caster.getHp(), "溢出部分不计入共鸣");
		caster.getReady().clear();
		target.setOwnerHero("other");
		target.setHp(100);
		heal(state);
		assertEquals(180, target.getHp());
		assertEquals(105, caster.getHp(), "其他成员的伙伴不触发自身共鸣");
	}

	@Test
	void thriftPaysDiscountedCostAndNourishOnlyChangesDirectHealing() {
		var state = state("节制");
		var caster = state.getUnits().get(0);
		caster.setMp(7);
		heal(state);
		assertEquals(0, caster.getMp());
		assertEquals(180, state.getUnits().get(1).getHp());
		state = state("滋养");
		heal(state);
		assertEquals(184, state.getUnits().get(1).getHp());
	}

	@Test
	void cleanFeatherRemovesSlowEvenIfHealOverflows() {
		var state = state("净羽");
		var target = state.getUnits().get(1);
		target.setHp(300);
		target.setSlowUntil(3);
		heal(state);
		assertEquals(0, target.getSlowUntil());
		assertEquals(300, target.getHp());
		assertEquals(1, state.getUnits().get(0).getCleanseRound());
	}

	@Test
	void voluntarySwapRegeneratesAndRelayOnlyTriggersOncePerBattle() {
		var state = state("再生力");
		var caster = state.getUnits().get(0);
		var reserve = unit("reserve", 2);
		state.getUnits().add(reserve);
		swap(state, "caster", "reserve");
		assertEquals(200, caster.getHp());
		assertEquals(2, caster.getPosition());
		state = state("接力守护");
		caster = state.getUnits().get(0);
		reserve = unit("reserve", 2);
		state.getUnits().add(reserve);
		swap(state, "caster", "reserve");
		assertTrue(caster.isRelayGuardUsed());
		assertEquals(2, reserve.getRelayGuardUntil());
		swap(state, "reserve", "caster");
		assertEquals(0, reserve.getRelayGuardUntil());
		swap(state, "caster", "reserve");
		assertEquals(0, reserve.getRelayGuardUntil(), "反复换宠不能刷新每场一次的保护");
	}

	@Test
	void replacementPreviewUsesCopiedAbilityForManaValidation() {
		var preview = state(null);
		preview.getUnits().getFirst().setHp(0);
		preview.getUnits().get(2).setAbility("节制");
		var reserve = unit("reserve", 2);
		reserve.setAbility("复制");
		reserve.setMp(7);
		preview.getUnits().add(reserve);
		engine.validateMember(preview, "owner",
				List.of(new BattleStateOrder("reserve", "微疗", "target"),
						new BattleStateOrder("target", "DEFEND", "target")),
				Map.of("reserve", 0), new BattleRulesTest.FakeResources());
		assertEquals("节制", reserve.effect());
		assertEquals(7, reserve.getMp(), "提交校验不提前消耗魔力");
	}

	private BattleState state(String ability) {
		var state = new BattleState();
		state.setEncounter("VILLAGE");
		var caster = unit("caster", 0);
		caster.setAbility(ability);
		var target = unit("target", 1);
		var enemy = unit("enemy", 0);
		enemy.setEnemy(true);
		state.setUnits(new ArrayList<>(List.of(caster, target, enemy)));
		return state;
	}

	private BattleStateUnit unit(String id, int position) {
		var unit = new BattleStateUnit();
		unit.setId(id);
		unit.setName(id);
		unit.setSpecies("CLAW_CAT");
		unit.setOwnerHero("owner");
		unit.setPosition(position);
		unit.setStats(new BattleStateStats(300, 40, 20, 80, 20, 20, 20));
		unit.setHp(100);
		unit.setMp(40);
		unit.setSkills(List.of("微疗"));
		return unit;
	}

	private void heal(BattleState state) {
		state.setEnemyOrders(List.of());
		BattleTestRounds.resolve(engine, state,
				List.of(new BattleStateOrder("caster", "微疗", "target"),
						new BattleStateOrder("target", "DEFEND", "target")),
				false, new BattleRulesTest.FakeResources());
	}

	private void swap(BattleState state, String outgoing, String incoming) {
		state.setEnemyOrders(List.of());
		BattleTestRounds.resolve(engine, state,
				List.of(new BattleStateOrder(outgoing, "SWAP", incoming),
						new BattleStateOrder("target", "DEFEND", "target")),
				false, new BattleRulesTest.FakeResources());
	}

}
