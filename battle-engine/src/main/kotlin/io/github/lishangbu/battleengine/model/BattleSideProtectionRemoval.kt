package io.github.lishangbu.battleengine.model

import io.github.lishangbu.battleengine.canBattle

/**
 * 一侧非伤害型防护被批量移除后的状态变更。
 *
 * `removedKinds` 保留移除前在目标侧实际存在的防护种类顺序。它只记录真实删除的状态，不把调用方请求删除但本来
 * 不存在的防护写入事件，避免 replay 误报场上发生过不存在的清理。
 */
data class BattleSideProtectionRemoval(
	val state: BattleState,
	val removedKinds: List<BattleSideProtectionKind>,
)
