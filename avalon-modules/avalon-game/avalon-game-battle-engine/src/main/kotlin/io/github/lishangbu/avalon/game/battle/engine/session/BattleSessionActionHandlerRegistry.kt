package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * `BattleSessionActionHandler` 注册中心。
 */
interface BattleSessionActionHandlerRegistry {
    /**
     * 根据当前 action 返回对应的执行处理器。
     *
     * @param action 当前待执行的 session action。
     * @return 与 action 类型匹配的处理器。
     */
    fun get(action: BattleSessionAction): BattleSessionActionHandler
}
