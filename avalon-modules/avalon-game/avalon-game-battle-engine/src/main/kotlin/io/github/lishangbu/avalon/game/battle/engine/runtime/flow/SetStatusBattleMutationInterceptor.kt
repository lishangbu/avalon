package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames
import io.github.lishangbu.avalon.game.battle.engine.mutation.BattleMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.SetStatusMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.apply.MutationApplicationContext
import io.github.lishangbu.avalon.game.battle.engine.runtime.support.MutationTargetSelectorResolver

/**
 * 主状态附加 mutation 拦截器。
 */
class SetStatusBattleMutationInterceptor : BattleMutationInterceptor {
    /**
     * 当前拦截器在链中的执行顺序。
     */
    override val order: Int = 0

    /**
     * 判断当前拦截器是否负责处理给定 mutation。
     */
    override fun supports(mutation: BattleMutation): Boolean = mutation is SetStatusMutation

    /**
     * 拦截主状态附加 mutation。
     */
    override fun intercept(
        context: BattleMutationInterceptionContext,
        attachedEffectProcessor: BattleAttachedEffectProcessor,
    ): BattleMutationInterceptionResult {
        val mutation = context.mutation as? SetStatusMutation ?: return BattleMutationInterceptionResult(context.snapshot, true)
        val targetUnitIds =
            MutationTargetSelectorResolver.resolve(
                mutation.target,
                mutationApplicationContext(context),
            )
        var currentSnapshot = context.snapshot
        var blocked = false

        targetUnitIds.forEach { affectedTargetId ->
            val result =
                attachedEffectProcessor.process(
                    snapshot = currentSnapshot,
                    unitId = affectedTargetId,
                    hookName = StandardHookNames.ON_SET_STATUS.value,
                    targetId = affectedTargetId,
                    sourceId = context.sourceId,
                    relay = true,
                    attributes = mapOf("statusId" to mutation.statusId, "targetRelation" to "foe"),
                )
            currentSnapshot = result.snapshot
            if (result.cancelled || result.relay == false) {
                blocked = true
            }
        }

        return BattleMutationInterceptionResult(
            snapshot = currentSnapshot,
            allowed = !blocked,
        )
    }

    /**
     * 组装 mutation target selector 所需的上下文对象。
     */
    private fun mutationApplicationContext(context: BattleMutationInterceptionContext): MutationApplicationContext =
        MutationApplicationContext(
            battle = context.snapshot.battle,
            field = context.snapshot.field,
            units = context.snapshot.units,
            selfId = context.selfId,
            targetId = context.targetId,
            sourceId = context.sourceId,
        )
}
