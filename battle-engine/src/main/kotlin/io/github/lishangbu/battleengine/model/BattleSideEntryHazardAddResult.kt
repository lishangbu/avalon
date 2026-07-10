package io.github.lishangbu.battleengine.model

/**
 * 一侧新增或叠加入场陷阱后的状态变更结果。
 *
 * [side] 是已经写入陷阱的下一版战斗侧，[hazard] 是最终生效的陷阱快照。引擎事件层使用该结果记录当前层数，
 * 从而区分“新建第一层”和“叠加到第二/第三层”，同时避免在未变化时产生误导性事件。
 */
data class BattleSideEntryHazardAddResult(
	val side: BattleSide,
	val hazard: BattleSideEntryHazard,
)
