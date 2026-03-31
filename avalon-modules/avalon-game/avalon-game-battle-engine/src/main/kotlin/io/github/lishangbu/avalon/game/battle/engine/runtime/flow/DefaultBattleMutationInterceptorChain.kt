package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.mutation.BattleMutation

/**
 * 默认 battle mutation 拦截链实现。
 *
 * @property interceptors 按顺序执行的 mutation 拦截器集合。
 */
class DefaultBattleMutationInterceptorChain(
    interceptors: List<BattleMutationInterceptor>,
) : BattleMutationInterceptorChain {
    private val interceptors: List<BattleMutationInterceptor> = interceptors.sortedBy(BattleMutationInterceptor::order)

    /**
     * 过滤一组待提交的 mutation。
     */
    override fun filter(
        snapshot: BattleRuntimeSnapshot,
        selfId: String?,
        targetId: String?,
        sourceId: String?,
        mutations: List<BattleMutation>,
        attachedEffectProcessor: BattleAttachedEffectProcessor,
    ): MutationFilteringResult {
        var currentSnapshot = snapshot
        val allowedMutations = mutableListOf<BattleMutation>()

        mutations.forEach { mutation ->
            var mutationSnapshot = currentSnapshot
            var allowed = true

            interceptors.forEach { interceptor ->
                if (!allowed || !interceptor.supports(mutation)) {
                    return@forEach
                }
                val result =
                    interceptor.intercept(
                        context =
                            BattleMutationInterceptionContext(
                                snapshot = mutationSnapshot,
                                selfId = selfId,
                                targetId = targetId,
                                sourceId = sourceId,
                                mutation = mutation,
                            ),
                        attachedEffectProcessor = attachedEffectProcessor,
                    )
                mutationSnapshot = result.snapshot
                if (!result.allowed) {
                    allowed = false
                }
            }

            currentSnapshot = mutationSnapshot
            if (allowed) {
                allowedMutations += mutation
            }
        }

        return MutationFilteringResult(
            snapshot = currentSnapshot,
            mutations = allowedMutations,
        )
    }
}
