package io.github.lishangbu.avalon.game.battle.engine.session.target

import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot

/**
 * 默认 battle session 目标查询服务。
 *
 * @property targetModeResolver effect 目标模式解析器。
 * @property availableTargetResolver 当前快照下的可选目标解析器。
 */
class DefaultBattleSessionTargetQueryService(
    private val targetModeResolver: BattleSessionTargetModeResolver,
    private val availableTargetResolver: BattleSessionAvailableTargetResolver,
) : BattleSessionTargetQueryService {
    /**
     * 基于当前快照生成一个 effect 的目标查询结果。
     */
    override fun resolve(
        snapshot: BattleRuntimeSnapshot,
        effectId: String,
        actorUnitId: String,
    ): BattleSessionTargetQuery {
        val targetMode = targetModeResolver.resolve(effectId)
        return BattleSessionTargetQuery(
            effectId = effectId,
            actorUnitId = actorUnitId,
            mode = targetMode,
            availableTargetUnitIds = availableTargetResolver.resolve(snapshot, actorUnitId, targetMode),
            requiresExplicitTarget = BattleSessionTargetingSupport.requiresExplicitTarget(targetMode),
        )
    }
}
