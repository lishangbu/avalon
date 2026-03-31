package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * battle 回合推进步骤。
 *
 * 设计意图：
 * - 把回合推进中的多个固定阶段拆成可排序、可替换的 pipeline step。
 * - 让后续新增 battle 生命周期步骤时不需要继续膨胀 `BattleSession.resolveTurn`。
 */
interface BattleSessionTurnStep {
    /**
     * 当前步骤的执行顺序；值越小越先执行。
     */
    val order: Int

    /**
     * 在指定上下文中执行当前回合步骤。
     *
     * @param context 本次回合推进的共享上下文。
     */
    fun execute(context: BattleSessionTurnContext)
}
