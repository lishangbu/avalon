package io.github.lishangbu.avalon.combat;

import io.github.lishangbu.avalon.combat.model.CombatPhase;

/** 游戏规则扩展。只接收隔离状态和外部输入；实现不得调用数据库、库存、网络或真实时钟。 */
public interface CombatRoundRules<S, I> {

	/** 深复制输入状态，保证校验或处理器失败时调用方原状态保持不变。 */
	S copy(S state);

	/** 返回服务器保存的本轮编号；核心拒绝在已经结束的战斗上执行新的回合。 */
	int round(S state);

	/** 判断战斗是否已终局。终局后核心跳过行动与回合末，只执行收尾。 */
	boolean terminal(S state);

	/** 执行唯一指定阶段；顺序与预算由核心控制，输入中不得提供可执行代码。 */
	void apply(CombatPhase phase, CombatFrame<S, I> frame);

}
