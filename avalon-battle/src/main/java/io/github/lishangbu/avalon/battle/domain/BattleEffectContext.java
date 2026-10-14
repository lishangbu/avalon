package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.content.battle.model.BattleSkill;
import java.util.*;

/** 单次技能效果的隔离上下文；费用在创建前支付一次，各效果不能再次扣费或修改队列。 */
public record BattleEffectContext(BattleState state, BattleStateUnit actor, BattleStateUnit target,
		List<BattleStateUnit> targets, BattleSkill skill, boolean area, Random random, Set<String> defending,
		Map<String, List<DirectAttackResolverProtection>> covers) {
	/** 目标集合在整次技能开始前锁定，不能由后一个效果重选或增加目标数量。 */
	public BattleEffectContext {
		targets = List.copyOf(targets);
	}
}
