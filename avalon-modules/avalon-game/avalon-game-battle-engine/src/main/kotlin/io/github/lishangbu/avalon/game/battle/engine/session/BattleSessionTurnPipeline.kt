package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * battle 回合推进 pipeline。
 *
 * 设计意图：
 * - 统一编排回合准备、动作执行和回合结束等固定阶段。
 * - 提供可注册、可替换的 session turn orchestration 入口。
 */
interface BattleSessionTurnPipeline {
    /**
     * 推进一次完整回合。
     *
     * @param session 当前 battle session。
     * @return 当前回合的汇总结果。
     */
    fun resolve(session: BattleSession): BattleSessionTurnResult
}
