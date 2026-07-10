package io.github.lishangbu.battleengine.model

/**
 * 战斗状态级别的一侧入场陷阱变更结果。
 *
 * 这是 [BattleState] 写入一侧状态时的返回值：调用方需要新的 [state] 继续结算，同时需要 [hazard] 生成事件。
 * 该类型让不可变状态更新和事件构造分离，避免 `BattleState` 自身理解技能、回合或事件文案。
 */
data class BattleSideEntryHazardStateChange(
	val state: BattleState,
	val hazard: BattleSideEntryHazard,
)
