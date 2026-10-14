package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.*;
import io.github.lishangbu.avalon.content.battle.model.BattleSkill;
import io.github.lishangbu.avalon.content.battle.model.BattleSkillEffect;
import io.github.lishangbu.avalon.content.item.PotionRecoveryRules;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 组合技能经过正式回合入口，验证效果顺序不决定目标形状，特性不丢弃其余效果。 */
class BattleCompositeSkillTest {

	/** 减速位于第二项时仍受特性转换，保留前项伤害并且只扣一次优惠费用。 */
	@Test
	void convertsSlowEffectWithoutDroppingOtherEffectsOrChargingTwice() {
		var skill = skill(List.of(new BattleSkillEffect("DAMAGE", 10), new BattleSkillEffect("SLOW", 20)), "ENEMY");
		var state = state();
		var actor = state.getUnits().getFirst();
		actor.setAbility("舍控强攻");
		actor.setMp(8);
		var result = engine(skill).advance(state, List.of(new BattleStateOrder("actor", "组合", "enemy")), false,
				new BattleResourceSnapshot(Map.of(), Set.of()), Map.of());
		assertEquals(0, result.state().getUnits().getFirst().getMp());
		assertEquals(0, result.state().getUnits().get(1).getSlowUntil());
		assertEquals(2, result.state().getLog().stream().filter(line -> line.contains("使用组合")).count());
		assertEquals("SLOW", skill.effects().get(1).kind());
		assertEquals(8, actor.getMp());
	}

	/** AREA不是首项也必须走明确多目标提交，目标集合一次锁定且不重复扣费。 */
	@Test
	void recognizesAreaEffectAfterAnotherEffect() {
		var skill = skill(List.of(new BattleSkillEffect("DAMAGE", 10), new BattleSkillEffect("AREA", 20)), "ENEMIES");
		var state = state();
		var enemy2 = BattleStateCodec.copy(state).getUnits().get(1);
		enemy2.setId("enemy2");
		enemy2.setName("enemy2");
		state.setUnits(List.of(state.getUnits().getFirst(), state.getUnits().get(1), enemy2));
		var result = engine(skill).advance(state,
				List.of(new BattleStateOrder("actor", "组合", "enemy", List.of("enemy", "enemy2"))), false,
				new BattleResourceSnapshot(Map.of(), Set.of()), Map.of());
		assertEquals(10, result.state().getUnits().getFirst().getMp());
		assertTrue(result.state().getUnits().get(1).getHp() < 1000);
		assertTrue(result.state().getUnits().get(2).getHp() < 1000);
	}

	/** 只替换目录中的测试技能，结算、状态、注册器及费用均使用正式实现。 */
	private BattleEngine engine(BattleSkill skill) {
		var fixture = new RuleTestFixture();
		var catalog = spy(fixture.battles);
		doReturn(skill).when(catalog).skill("组合");
		return new BattleEngine(new PotionRecoveryRules(fixture.items), catalog, fixture.rules, fixture.balls,
				fixture.training, new BattleSkillEffects(fixture.attacks, catalog, List.of()), fixture.enemies,
				List.of());
	}

	/** 固定配置使费用与冷却可独立观察，攻击值不会提前终结战斗。 */
	private BattleSkill skill(List<BattleSkillEffect> effects, String target) {
		return new BattleSkill("组合", effects, "无", "PA", "PD", 10, 1, 0, target, null, null);
	}

	/** 两个最小单位拥有完整属性与身份，敌方本轮无动作以排除额外伤害。 */
	private BattleState state() {
		var state = new BattleState();
		state.setEncounter("VILLAGE");
		var actor = new BattleStateUnit();
		actor.setId("actor");
		actor.setName("actor");
		actor.setHero(true);
		actor.setLevel(5);
		actor.setHp(1000);
		actor.setMp(20);
		actor.setSkills(List.of("组合"));
		actor.setStats(new BattleStateStats(1000, 20, 100, 100, 100, 100, 30));
		var enemy = BattleStateCodec.copy(unitState(actor)).getUnits().getFirst();
		enemy.setId("enemy");
		enemy.setName("enemy");
		enemy.setHero(false);
		enemy.setEnemy(true);
		enemy.setAbility(null);
		enemy.setSkills(List.of());
		state.setUnits(List.of(actor, enemy));
		return state;
	}

	/** 使用正式深复制构造互不共享的测试单位。 */
	private BattleState unitState(BattleStateUnit unit) {
		var state = new BattleState();
		state.setUnits(List.of(unit));
		return state;
	}

}
