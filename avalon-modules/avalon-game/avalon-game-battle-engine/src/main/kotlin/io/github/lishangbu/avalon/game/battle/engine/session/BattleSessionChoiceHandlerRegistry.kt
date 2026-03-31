package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * `BattleSessionChoiceHandler` 注册中心。
 *
 * 设计意图：
 * - 为 `BattleSessionChoice` 提供稳定的查找入口。
 * - 让其他模块可以通过注入 registry 扩展新的 choice 处理策略。
 */
interface BattleSessionChoiceHandlerRegistry {
    /**
     * 根据当前 choice 返回对应的处理器。
     *
     * @param choice 当前正在提交的 choice。
     * @return 与 choice 类型匹配的处理器。
     */
    fun get(choice: BattleSessionChoice): BattleSessionChoiceHandler
}
