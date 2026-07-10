package io.github.lishangbu.battleengine.model

/**
 * 一场战斗的确定性复盘材料。
 *
 * Replay 的职责不是替代战斗状态存档，而是把“从什么初始快照开始、每回合提交了哪些行动、随机源每次返回
 * 了什么、最终产生了哪些事件”作为一组可审计事实保存下来。后续接入公开规则对照测试、CI 对照测试或管理端
 * 运行记录时，可以把该对象作为最小复盘单元。
 *
 * 该模型刻意不保存数据库 ID、资料原始 JSON 或外部文本。它只包含引擎已经冻结的运行态对象，因此同一份
 * replay 可以在不访问数据库的情况下重新结算。若规则实现发生变化，严格回放会比较每回合事件和最终状态，
 * 帮助定位具体从哪一次随机消费或事件开始偏离。
 *
 * @property initialState 战斗开始前的完整冻结快照。
 * @property initialEvents `BattleEngine.start` 产生的启动阶段事件，通常包含战斗开始和出场规则事件。
 * @property turns 每个已经结算回合的行动、随机 trace 和事件片段。
 * @property finalState 结算完所有回合后的最终状态；其事件流必须等于 `initialEvents + turns.events`。
 */
data class BattleReplay(
	val initialState: BattleInitialState,
	val initialEvents: List<BattleEvent>,
	val turns: List<BattleReplayTurn>,
	val finalState: BattleState,
) {
	init {
		require(initialEvents.all { it.turnNumber == 0 }) { "initialEvents must belong to turn 0" }
		require(turns.map { it.turnNumber } == (1..turns.size).toList()) {
			"turn numbers must be continuous from 1"
		}
		val expectedFinalTurnNumber = turns.lastOrNull()?.turnNumber ?: 0
		require(finalState.turnNumber == expectedFinalTurnNumber) {
			"finalState turnNumber must match the last replay turn"
		}
		val replayEvents = initialEvents + turns.flatMap { it.events }
		require(finalState.events == replayEvents) {
			"finalState events must equal initial events plus replay turn events"
		}
	}
}
