package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

/**
 * 默认单次出招主流程 pipeline。
 *
 * @property steps 按顺序执行的 move resolution step 集合。
 */
class DefaultBattleMoveResolutionPipeline(
    steps: List<BattleMoveResolutionStep>,
) : BattleMoveResolutionPipeline {
    private val steps: List<BattleMoveResolutionStep> = steps.sortedBy(BattleMoveResolutionStep::order)

    /**
     * 按既定顺序推进一次完整的 move resolution。
     */
    override fun resolve(context: BattleMoveResolutionContext): MoveResolutionResult {
        steps.forEach { step ->
            if (context.cancelled) {
                return@forEach
            }
            step.execute(context)
        }
        return context.toResult()
    }
}
