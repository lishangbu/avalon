package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.model.BattleState
import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.SideState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState

/**
 * battle 主流程运行时快照。
 *
 * 设计意图：
 * - 作为主流程入口与 mutation apply 层之间的稳定状态载体。
 * - 把 battle / field / side / unit 状态收拢为不可变快照。
 *
 * @property battle 全局 battle 状态。
 * @property field 当前场地状态。
 * @property units 全部单位状态表。
 * @property sides 当前 side 状态表。
 */
data class BattleRuntimeSnapshot(
    val battle: BattleState,
    val field: FieldState,
    val units: Map<String, UnitState>,
    val sides: Map<String, SideState> = emptyMap(),
)
