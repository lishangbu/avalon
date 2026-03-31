package io.github.lishangbu.avalon.game.battle.engine.runtime.apply

import io.github.lishangbu.avalon.game.battle.engine.model.BattleState
import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.SideState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.type.HookName

/**
 * mutation 应用完成后的结果。
 *
 * @property battle 更新后的 battle 状态。
 * @property field 更新后的 field 状态。
 * @property units 更新后的单位状态表。
 * @property side 更新后的 side 状态。
 * @property foeSide 更新后的 foe side 状态。
 * @property triggeredHooks 因 mutation 请求而触发的后续 Hook 集合。
 */
data class MutationApplicationResult(
    val battle: BattleState,
    val field: FieldState,
    val units: Map<String, UnitState>,
    val side: SideState? = null,
    val foeSide: SideState? = null,
    val triggeredHooks: List<HookName> = emptyList(),
)
