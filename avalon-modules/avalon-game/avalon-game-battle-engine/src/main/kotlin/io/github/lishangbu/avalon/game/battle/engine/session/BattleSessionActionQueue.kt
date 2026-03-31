package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * BattleSession 的最小动作队列。
 *
 * 设计意图：
 * - 为一回合内多个 move action 提供收集与顺序执行能力。
 * - 当前采用最小可用排序规则：
 *   1. priority 降序
 *   2. speed 降序
 *   3. action type order 升序
 *   4. enqueueOrder 升序
 *
 * @property actionSortingStrategy 当前队列使用的 action 排序策略。
 */
class BattleSessionActionQueue(
    private val actionSortingStrategy: BattleSessionActionSortingStrategy,
) {
    private val actions: MutableList<QueuedBattleSessionMoveAction> = mutableListOf()
    private var nextEnqueueOrder: Long = 0

    /**
     * 入队一个 move action。
     */
    fun enqueue(action: BattleSessionAction) {
        actions +=
            QueuedBattleSessionMoveAction(
                action = action,
                enqueueOrder = nextEnqueueOrder++,
            )
    }

    /**
     * 返回当前待执行动作的只读快照。
     */
    fun snapshot(): List<BattleSessionAction> = actions.map(QueuedBattleSessionMoveAction::action)

    /**
     * 取出并清空当前队列。
     */
    fun drain(): List<BattleSessionAction> {
        val drained = actionSortingStrategy.sort(actions)
        actions.clear()
        return drained
    }

    /**
     * 以给定动作列表重建当前队列。
     */
    fun replaceAll(newActions: List<BattleSessionAction>) {
        actions.clear()
        nextEnqueueOrder = 0
        newActions.forEach(::enqueue)
    }

    /**
     * 当前队列是否为空。
     */
    fun isEmpty(): Boolean = actions.isEmpty()
}
