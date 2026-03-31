package io.github.lishangbu.avalon.game.battle.engine.session

import kotlin.reflect.KClass

/**
 * `BattleSessionChoice` 提交策略。
 *
 * 设计意图：
 * - 把不同 choice 的校验、入队与日志记录逻辑拆成可独立注册的策略组件。
 * - 让 `BattleSession.submitChoice` 不再依赖集中式 `when` 分发。
 *
 * 线程安全约束：
 * - 实现类应保持无状态，便于注册为 Spring 单例 Bean。
 */
interface BattleSessionChoiceHandler {
    /**
     * 当前处理器负责的 choice 类型。
     */
    val choiceType: KClass<out BattleSessionChoice>

    /**
     * 提交一个 choice 到指定 session。
     *
     * @param choice 本次提交的统一 choice 对象。
     * @param session 当前会话对象；其内部状态会在提交过程中被更新。
     * @return 更新后的待执行动作队列快照。
     */
    fun submit(
        choice: BattleSessionChoice,
        session: BattleSession,
    ): List<BattleSessionAction>
}
