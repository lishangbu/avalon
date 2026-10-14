package io.github.lishangbu.avalon.battle.domain;

import java.util.*;
import io.github.lishangbu.avalon.combat.CombatFrame;

/** 单次回合的瞬时阶段数据，不持久化、不作为HTTP响应；只能在完整回合执行期间使用。 */
final class BattleRoundSession {

	/** 本次隔离状态由核心复制后赋值，原战局不能在校验阶段被改写。 */
	BattleState state;

	/** 玩家原始指令固定保存，不与敌方意图共享可变列表。 */
	final List<BattleStateOrder> input;

	/** 主动认输仍进入核心收尾，不能在退出后继续跳状态伤害。 */
	final boolean surrender;

	/** 资源执行仅使用快照账本，规则不能直接执行真实资产写入。 */
	final BattleEngineResources resources;

	/** 免费补位绑定具体单位编号，而不是依靠位置继承指令。 */
	final Map<String, Integer> replacements;

	/** 锁定后的行动列表，各阶段只能处理自己的动作，不重新生成敌方指令。 */
	final List<BattleStateOrder> orders = new ArrayList<>();

	/** 防御与保护持续本轮，SWAPS及ACTIONS必须共享同一上下文。 */
	final Set<String> defending = new HashSet<>();

	final Map<String, List<DirectAttackResolverProtection>> covers = new HashMap<>();

	/** 核心提供可追踪流；不能在每个动作上重新初始化种子。 */
	Random random;

	/** 核心帧携带当前阶段与触发预算，效果必须通过它执行。 */
	CombatFrame<BattleState, BattleRoundInput> frame;

	/** 建立完整输入，集合复制避免调用方在结算途中修改指令或补位。 */
	BattleRoundSession(BattleState state, List<BattleStateOrder> input, boolean surrender,
			BattleEngineResources resources, Map<String, Integer> replacements) {
		this.state = state;
		this.input = input == null ? null : List.copyOf(input);
		this.surrender = surrender;
		this.resources = resources;
		this.replacements = Map.copyOf(replacements);
	}

}
