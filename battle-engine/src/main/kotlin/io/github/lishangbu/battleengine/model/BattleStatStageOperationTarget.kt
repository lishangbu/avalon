package io.github.lishangbu.battleengine.model

/**
 * 能力阶级操作的参与对象范围。
 *
 * `USER` 和 `TARGET` 分别指本次技能使用者与实际命中的目标。`ALL_ACTIVE` 用于黑雾这类全场清除效果，
 * 只作为清除操作的目标范围；复制和交换必须有明确的单个来源与目标，避免双打中产生歧义。
 */
enum class BattleStatStageOperationTarget {
	USER,
	TARGET,
	ALL_ACTIVE,
}

