package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.mutation.BattleMutation

/**
 * battle mutation 拦截器。
 *
 * 设计意图：
 * - 把状态附加、挥发状态附加等会被前置 hook 拦截的 mutation 判断逻辑拆成独立组件。
 * - 允许通过链式组合方式逐步扩展 mutation interception 行为。
 */
interface BattleMutationInterceptor {
    /**
     * 当前拦截器在链中的执行顺序；值越小越先执行。
     */
    val order: Int

    /**
     * 判断当前拦截器是否负责处理给定 mutation。
     *
     * @param mutation 当前待判断的 mutation。
     * @return 当前拦截器是否需要参与本次拦截。
     */
    fun supports(mutation: BattleMutation): Boolean

    /**
     * 拦截当前 mutation。
     *
     * @param context 当前 mutation 的拦截上下文。
     * @param attachedEffectProcessor 触发挂载 effect 响应式 hook 的回调。
     * @return 当前拦截器的处理结果。
     */
    fun intercept(
        context: BattleMutationInterceptionContext,
        attachedEffectProcessor: BattleAttachedEffectProcessor,
    ): BattleMutationInterceptionResult
}
