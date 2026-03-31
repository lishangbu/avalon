package io.github.lishangbu.avalon.game.battle.engine.type

/**
 * 第一版标准 relay 类型集合。
 *
 * 设计意图：
 * - 为标准 HookSpec 提供稳定的 relay 语义标识。
 * - 让 hook 元信息与运行时解释保持同一套命名。
 */
object StandardRelayTypeIds {
    val NONE: RelayTypeId = RelayTypeId("none")
    val BOOLEAN: RelayTypeId = RelayTypeId("boolean")
    val INTEGER: RelayTypeId = RelayTypeId("integer")
    val DECIMAL: RelayTypeId = RelayTypeId("decimal")
    val OBJECT: RelayTypeId = RelayTypeId("object")
}
