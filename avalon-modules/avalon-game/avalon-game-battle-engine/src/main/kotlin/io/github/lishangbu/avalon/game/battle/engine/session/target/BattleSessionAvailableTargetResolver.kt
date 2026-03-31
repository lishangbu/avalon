package io.github.lishangbu.avalon.game.battle.engine.session.target

import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot

/**
 * 当前快照下的可选目标解析器。
 */
interface BattleSessionAvailableTargetResolver {
    /**
     * 解析指定目标模式下当前可选的目标单位列表。
     *
     * @param snapshot 当前 battle snapshot。
     * @param actorUnitId 当前出手单位标识。
     * @param mode 已解析好的目标模式。
     * @return 当前快照下允许选择的目标单位标识列表。
     */
    fun resolve(
        snapshot: BattleRuntimeSnapshot,
        actorUnitId: String,
        mode: BattleSessionTargetMode,
    ): List<String>
}
