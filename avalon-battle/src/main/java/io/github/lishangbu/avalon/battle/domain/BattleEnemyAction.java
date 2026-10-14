package io.github.lishangbu.avalon.battle.domain;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** 一次已锁定的敌方行动上下文；策略共享本轮防御、保护及可追踪随机流，不重新抽取玩家输入。 */
public record BattleEnemyAction(BattleState state, BattleStateUnit actor, BattleStateUnit target,
		BattleStateOrder order, Random random, Set<String> defending,
		Map<String, List<DirectAttackResolverProtection>> covers) {
}
