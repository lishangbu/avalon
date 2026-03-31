package io.github.lishangbu.avalon.game.battle.engine.event

import io.github.lishangbu.avalon.game.battle.engine.dsl.EffectDefinition
import io.github.lishangbu.avalon.game.battle.engine.model.BattleState
import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.SideState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.type.HookName

/**
 * 单次 Hook 触发时的运行时上下文。
 *
 * 设计意图：
 * - 为条件解释器、动作执行器、special handler 提供统一读取入口。
 * - 把 battle、side、unit、effect、relay 等信息聚合为只读上下文对象。
 *
 * 该类型不负责状态变更，也不负责事件分发。
 *
 * @property hookName 当前触发的 Hook 名称。
 * @property battle 当前 battle 状态。
 * @property self 当前 handler 的主要持有者。
 * @property target 当前事件目标。
 * @property source 当前事件来源。
 * @property side 当前上下文关联的 side。
 * @property foeSide 当前上下文关联的对方 side。
 * @property field 当前场地状态。
 * @property effect 当前参与结算的 effect 定义。
 * @property relay 当前事件链上传递的 relay 值。
 * @property attributes 扩展属性字典，用于补充非固定上下文。
 */
data class EventContext(
    val hookName: HookName,
    val battle: BattleState,
    val self: UnitState? = null,
    val target: UnitState? = null,
    val source: UnitState? = null,
    val side: SideState? = null,
    val foeSide: SideState? = null,
    val field: FieldState? = null,
    val effect: EffectDefinition? = null,
    val relay: Any? = null,
    val attributes: Map<String, Any?> = emptyMap(),
)
