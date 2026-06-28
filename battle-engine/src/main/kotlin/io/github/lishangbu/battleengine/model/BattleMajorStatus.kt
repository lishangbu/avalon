package io.github.lishangbu.battleengine.model

/**
 * 主要异常状态。
 *
 * 当前已覆盖现代 MVP 需要的公共行为：灼伤降低物理伤害并在回合末扣血，麻痹降低速度并在行动前
 * 按固定概率阻止技能，中毒和剧毒在回合末扣血，睡眠按随机剩余阻止行动次数阻止技能行动。
 * 冰冻和更多解除来源会在状态处理器扩展批次中补齐。
 */
enum class BattleMajorStatus {
	BURN,
	PARALYSIS,
	POISON,
	BAD_POISON,
	SLEEP,
	FREEZE,
}
