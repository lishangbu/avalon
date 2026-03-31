package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

/**
 * 命中判定 phase step。
 *
 * @property hitResolutionPolicy battle 命中判定策略。
 */
class BattleMoveHitResolutionStep(
    private val hitResolutionPolicy: BattleHitResolutionPolicy,
) : BattleMoveResolutionStep {
    /**
     * 当前步骤在 pipeline 中的执行顺序。
     */
    override val order: Int = 200

    /**
     * 计算当前出招是否命中。
     */
    override fun execute(context: BattleMoveResolutionContext) {
        context.hitSuccessful =
            hitResolutionPolicy.determine(
                accuracy = context.accuracy,
                evasion = context.evasion,
                attributes = context.attributes,
            )
    }
}
