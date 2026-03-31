package io.github.lishangbu.avalon.game.battle.engine.dsl

import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId

/**
 * 动作 DSL 节点的统一接口。
 *
 * 设计意图：
 * - 为所有动作 AST 节点提供统一入口。
 * - 通过 [type] 与动作执行器 registry 建立分发关系。
 *
 * 该接口只描述“动作数据”，不包含执行逻辑。
 *
 * @property type 动作节点类型标识。
 */
interface ActionNode {
    val type: ActionTypeId
}
