package io.github.lishangbu.avalon.game.battle.engine.session.target

import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot

/**
 * 默认可选目标解析器。
 */
class DefaultBattleSessionAvailableTargetResolver : BattleSessionAvailableTargetResolver {
    /**
     * 解析指定目标模式下当前可选的目标单位列表。
     */
    override fun resolve(
        snapshot: BattleRuntimeSnapshot,
        actorUnitId: String,
        mode: BattleSessionTargetMode,
    ): List<String> =
        BattleSessionTargetingSupport.availableTargetUnitIds(
            mode = mode,
            actorUnitId = actorUnitId,
            sides = snapshot.sides.values,
        )
}
