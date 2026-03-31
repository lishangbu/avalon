package io.github.lishangbu.avalon.game.battle.engine.dsl

import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId

/**
 * 条件 DSL 节点的统一接口。
 *
 * 设计意图：
 * - 为所有条件 AST 节点提供统一入口。
 * - 通过 [type] 与解释器 registry 建立分发关系。
 *
 * 该接口只描述“节点是什么”，不包含求值逻辑。
 *
 * @property type 条件节点类型标识。
 */
interface ConditionNode {
    val type: ConditionTypeId
}
