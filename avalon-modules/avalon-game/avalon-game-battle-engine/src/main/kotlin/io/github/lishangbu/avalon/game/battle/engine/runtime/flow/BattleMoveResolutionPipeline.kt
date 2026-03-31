package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

/**
 * 单次出招主流程 pipeline。
 */
interface BattleMoveResolutionPipeline {
    /**
     * 推进一次完整的 move resolution。
     *
     * @param context 本次 move resolution 的共享上下文。
     * @return 最终结算结果。
     */
    fun resolve(context: BattleMoveResolutionContext): MoveResolutionResult
}
