package io.github.lishangbu.battleengine.model

import io.github.lishangbu.battleengine.canBattle

/**
 * 一侧伤害减免屏障被批量移除后的状态变更。
 *
 * `removedKinds` 保留移除前在目标侧实际存在的屏障种类顺序，事件层可以直接使用它生成稳定 replay。它不是调用方
 * 请求删除的 kind 集合；这样当目标侧只存在物理屏障时，不会误报特殊屏障或全伤害屏障也被删除。
 */
data class BattleSideDamageReductionRemoval(
	val state: BattleState,
	val removedKinds: List<BattleSideDamageReductionKind>,
)
