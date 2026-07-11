package io.github.lishangbu.battlesession.model

/**
 * 汇总下一回合必须由调用方提供的全部人工选择。
 *
 * 空集合表示当前状态没有人工选择，不代表调用方可以省略已公开的其他要求。
 */
data class TurnRequirements(
	val selections: List<TurnSelectionRequirement>,
)
