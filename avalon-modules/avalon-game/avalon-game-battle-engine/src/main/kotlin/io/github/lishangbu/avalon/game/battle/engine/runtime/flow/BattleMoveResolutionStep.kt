package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

/**
 * 单次出招主流程的 pipeline step。
 *
 * 设计意图：
 * - 把 move resolution 中固定顺序的多个阶段拆成独立步骤。
 * - 允许后续通过新增或替换 step 的方式扩展 battle flow。
 */
interface BattleMoveResolutionStep {
    /**
     * 当前步骤在 pipeline 中的执行顺序；值越小越先执行。
     */
    val order: Int

    /**
     * 在指定上下文中执行当前步骤。
     *
     * @param context 本次 move resolution 的共享上下文。
     */
    fun execute(context: BattleMoveResolutionContext)
}
