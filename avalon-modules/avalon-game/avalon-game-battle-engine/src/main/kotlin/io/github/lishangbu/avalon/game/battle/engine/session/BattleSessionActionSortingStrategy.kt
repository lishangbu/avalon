package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * battle session action 排序策略。
 *
 * 设计意图：
 * - 把 action queue 的排序规则从队列容器中拆出。
 * - 让不同 battle 规则集可以替换回合内 action 的结算顺序。
 */
interface BattleSessionActionSortingStrategy {
    /**
     * 对当前队列中的 action 进行排序。
     *
     * @param actions 当前队列中包含入队顺序信息的 action 集合。
     * @return 排序后的 action 列表。
     */
    fun sort(actions: List<QueuedBattleSessionMoveAction>): List<BattleSessionAction>
}
