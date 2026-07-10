package io.github.lishangbu.battleengine.model

import io.github.lishangbu.battleengine.canBattle

/**
 * 一侧入场陷阱被批量清除后的状态变更。
 *
 * `removedHazards` 是移除前该侧实际存在的陷阱快照，保留原始顺序和层数。清场技能会用它追加稳定事件；调用方不必
 * 重新读取已经清空的 side，也不会误报本来不存在的陷阱。
 */
data class BattleSideEntryHazardRemoval(
	val state: BattleState,
	val removedHazards: List<BattleSideEntryHazard>,
)
