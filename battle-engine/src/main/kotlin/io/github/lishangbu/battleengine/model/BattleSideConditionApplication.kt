package io.github.lishangbu.battleengine.model

/**
 * 技能命中后尝试施加到某一侧场上的持续效果。
 *
 * 该模型目前只承载已经进入引擎的防守方伤害减免屏障，例如物理屏障、特殊屏障和同时覆盖两类标准伤害的屏障。
 * 目标侧用枚举表达，避免在纯引擎里解析资料表中的 `USER_SIDE`、`TARGET_SIDE` 字符串。
 *
 * 持续回合保存在 [BattleSideDamageReduction] 中，由回合末统一递减；触发概率保留在这里，便于后续表达命中后
 * 才有概率建立的一侧场上效果。`requiredWeather` 用于表达极光类屏障必须在指定天气中成功建立的现代规则。
 * 当前公开规则中的普通屏障类技能通常为 100%。
 */
data class BattleSideConditionApplication(
	val targetSide: BattleSideConditionTarget,
	val damageReduction: BattleSideDamageReduction,
	val chancePercent: Int = 100,
	val requiredWeather: BattleWeather? = null,
) {
	init {
		require(chancePercent in 0..100) { "chancePercent must be between 0 and 100" }
		require(requiredWeather != BattleWeather.NONE) { "requiredWeather cannot be NONE" }
	}
}

/**
 * 技能一侧场上效果的目标侧。
 */
enum class BattleSideConditionTarget {
	USER_SIDE,
	TARGET_SIDE,
}
