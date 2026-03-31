package io.github.lishangbu.avalon.game.battle.engine.session

import kotlin.reflect.KClass

/**
 * `BattleSessionAction` 执行策略。
 *
 * 设计意图：
 * - 把不同 action 的执行逻辑拆成可独立注册的策略组件。
 * - 让 `BattleSessionActionExecutor` 只负责队列遍历和生命周期控制。
 *
 * 线程安全约束：
 * - 实现类应保持无状态，便于注册为 Spring 单例 Bean。
 */
interface BattleSessionActionHandler {
    /**
     * 当前处理器负责的 action 类型。
     */
    val actionType: KClass<out BattleSessionAction>

    /**
     * 执行一个 session action。
     *
     * @param action 当前要执行的 action。
     * @param session 当前会话对象；其内部状态会在执行过程中被更新。
     * @return 该 action 的最终执行结果。
     */
    fun execute(
        action: BattleSessionAction,
        session: BattleSession,
    ): BattleSessionActionExecutionResult
}
