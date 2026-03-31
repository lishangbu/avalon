package io.github.lishangbu.avalon.game.battle.engine.event

import io.github.lishangbu.avalon.game.battle.engine.type.HookName
import io.github.lishangbu.avalon.game.battle.engine.type.RelayTypeId

/**
 * Hook 元信息定义。
 *
 * 设计意图：
 * - 明确每个 Hook 的 relay 语义与能力边界。
 * - 防止不同 Hook 对“能否改 relay / 能否取消”理解不一致。
 *
 * @property name Hook 名称。
 * @property relayType 当前 Hook 的 relay 语义标识。
 * @property supportsRelayMutation 当前 Hook 是否允许修改 relay。
 * @property supportsCancellation 当前 Hook 是否允许取消后续流程。
 */
data class HookSpec(
    val name: HookName,
    val relayType: RelayTypeId,
    val supportsRelayMutation: Boolean,
    val supportsCancellation: Boolean,
)
