package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 默认 battle session action 排序策略。
 *
 * 设计意图：
 * - 保留当前引擎使用的最小动作排序语义。
 * - 为后续引入 battle type 差异化排序规则预留扩展点。
 */
class DefaultBattleSessionActionSortingStrategy : BattleSessionActionSortingStrategy {
    /**
     * 对当前队列中的 action 进行排序。
     */
    override fun sort(actions: List<QueuedBattleSessionMoveAction>): List<BattleSessionAction> =
        actions
            .sortedWith(
                compareByDescending<QueuedBattleSessionMoveAction> { queued -> queued.action.priority }
                    .thenByDescending { queued -> queued.action.speed }
                    .thenBy { queued -> queued.action.kind.sortOrder }
                    .thenBy { queued -> queued.enqueueOrder },
            ).map(QueuedBattleSessionMoveAction::action)
}
