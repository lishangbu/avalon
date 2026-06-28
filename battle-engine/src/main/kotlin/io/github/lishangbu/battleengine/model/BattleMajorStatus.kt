package io.github.lishangbu.battleengine.model

/**
 * 主要异常状态。
 *
 * 第一批状态实现只覆盖现代单打 MVP 最需要的公共行为：灼伤降低物理伤害并在回合末扣血，
 * 麻痹降低速度，中毒在回合末扣血。睡眠、冰冻、剧毒递增计数和状态解除会在状态处理器扩展批次中补齐。
 */
enum class BattleMajorStatus {
	BURN,
	PARALYSIS,
	POISON,
	BAD_POISON,
	SLEEP,
	FREEZE,
}
