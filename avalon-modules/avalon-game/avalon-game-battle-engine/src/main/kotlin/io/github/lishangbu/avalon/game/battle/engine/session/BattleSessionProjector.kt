package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * battle session 发布事件投影器。
 *
 * 设计意图：
 * - 把 `BattleSessionPublication` 投影到 battle log、event log、resource ledger 等不同读模型。
 * - 保持 projector 无状态，便于作为 Spring 单例 Bean 注册。
 */
interface BattleSessionProjector {
    /**
     * 当前 projector 在发布链中的执行顺序；值越小越先执行。
     */
    val order: Int

    /**
     * 把当前发布事件投影到指定 session 的读模型上。
     *
     * @param session 当前 battle session。
     * @param publication 本次发布的事件载体。
     */
    fun project(
        session: BattleSession,
        publication: BattleSessionPublication,
    )
}
