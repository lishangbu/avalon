package io.github.lishangbu.avalon.game.battle.engine.runtime.apply

import io.github.lishangbu.avalon.game.battle.engine.model.BattleState
import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.SideState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState

/**
 * mutation 应用阶段的上下文。
 *
 * 设计意图：
 * - 为 MutationApplier 提供完整的状态快照与 selector 解析信息。
 * - 把“状态应用”从 EventContext 解耦，避免 runtime 读上下文和写状态强绑定。
 *
 * @property battle 当前 battle 状态。
 * @property field 当前场地状态。
 * @property units 当前可见单位状态表，键为单位 id。
 * @property selfId 当前 self 单位 id。
 * @property targetId 当前 target 单位 id。
 * @property sourceId 当前 source 单位 id。
 * @property side 当前 side 状态。
 * @property foeSide 当前 foe side 状态。
 */
data class MutationApplicationContext(
    val battle: BattleState,
    val field: FieldState,
    val units: Map<String, UnitState>,
    val selfId: String? = null,
    val targetId: String? = null,
    val sourceId: String? = null,
    val side: SideState? = null,
    val foeSide: SideState? = null,
)
