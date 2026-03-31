package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 默认 battle 回合推进 pipeline。
 *
 * @property steps 按顺序执行的回合步骤集合。
 */
class DefaultBattleSessionTurnPipeline(
    steps: List<BattleSessionTurnStep>,
) : BattleSessionTurnPipeline {
    private val steps: List<BattleSessionTurnStep> = steps.sortedBy(BattleSessionTurnStep::order)

    /**
     * 按既定顺序推进一次完整回合。
     */
    override fun resolve(session: BattleSession): BattleSessionTurnResult {
        val context = BattleSessionTurnContext(session)
        steps.forEach { step -> step.execute(context) }
        session.recordLog("Resolved turn with ${context.actionResults.size} action(s).")
        session.recordEvent(
            BattleSessionTurnResolvedPayload(actionCount = context.actionResults.size),
        )
        return BattleSessionTurnResult(
            actionResults = context.actionResults,
            snapshot = context.snapshot,
        )
    }

    companion object {
        /**
         * 创建 battle-engine 内置的默认回合推进 pipeline。
         *
         * @return 包含标准回合步骤的默认 pipeline。
         */
        fun createDefault(): DefaultBattleSessionTurnPipeline =
            DefaultBattleSessionTurnPipeline(
                steps =
                    listOf(
                        BattleSessionTurnValidationStep(),
                        BattleSessionQueuedActionExecutionStep(),
                        BattleSessionTurnAdvanceStep(),
                    ),
            )
    }
}
