package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 队列内部使用的 move action 包装对象。
 *
 * @property action 原始 move action。
 * @property enqueueOrder 入队顺序，用于保证排序稳定性。
 */
data class QueuedBattleSessionMoveAction(
    val action: BattleSessionAction,
    val enqueueOrder: Long,
)
