package io.github.lishangbu.battleengine.model

/**
 * 一个已结算回合的复盘片段。
 *
 * 回合片段只保存调用方提交给引擎的原始行动，而不是引擎内部推导出的强制锁招、重定向目标或伤害细节。
 * 这些内部结果都应通过事件流表达。这样可以保证 replay 仍然以“玩家输入 + 随机 trace + 初始状态”为核心，
 * 不会把实现过程中的临时计划对象泄漏为长期契约。
 *
 * @property turnNumber 回合编号，从 1 开始，与事件中的 `turnNumber` 对齐。
 * @property submittedActions 调用方在本回合提交的行动列表，顺序保持原样。
 * @property randomTrace 本回合随机消费记录，序号必须从 1 连续递增。
 * @property events 本回合新增的事件片段，不包含之前回合的历史事件。
 */
data class BattleReplayTurn(
	val turnNumber: Int,
	val submittedActions: List<BattleAction>,
	val randomTrace: List<BattleRandomTraceEntry>,
	val events: List<BattleEvent>,
) {
	init {
		require(turnNumber > 0) { "turnNumber must be positive" }
		require(submittedActions.map { it.actorId }.toSet().size == submittedActions.size) {
			"submittedActions can contain at most one action per actor"
		}
		require(randomTrace.map { it.sequence } == (1..randomTrace.size).toList()) {
			"randomTrace sequence must be continuous from 1"
		}
		require(events.all { it.turnNumber == turnNumber }) {
			"turn events must belong to this turn"
		}
	}
}
