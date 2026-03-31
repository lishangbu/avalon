package io.github.lishangbu.avalon.game.battle.engine.session.target

import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot

/**
 * battle session 目标查询服务。
 */
interface BattleSessionTargetQueryService {
    /**
     * 基于当前快照生成一个 effect 的目标查询结果。
     *
     * @param snapshot 当前 battle snapshot。
     * @param effectId 被查询的 effect 标识。
     * @param actorUnitId 当前出手单位标识。
     * @return 对外可读的目标查询结果。
     */
    fun resolve(
        snapshot: BattleRuntimeSnapshot,
        effectId: String,
        actorUnitId: String,
    ): BattleSessionTargetQuery
}
