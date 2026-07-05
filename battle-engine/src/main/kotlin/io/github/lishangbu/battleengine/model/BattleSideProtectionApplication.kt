package io.github.lishangbu.battleengine.model

/**
 * 技能命中后尝试施加到某一侧的防护效果。
 *
 * 防护效果与伤害屏障、顺风、入场陷阱共用“作用到使用者侧或目标侧”的规则，但它本身不改变伤害、速度或换入
 * 结算。单独建模可以让白雾/神秘守护这类技能在纯引擎里保持强类型，而不是把 `effect_policy` 字符串带进战斗
 * 结算流程。
 */
data class BattleSideProtectionApplication(
	val targetSide: BattleSideConditionTarget,
	val protection: BattleSideProtection,
	val chancePercent: Int = 100,
	val requiredWeather: BattleWeather? = null,
) {
	init {
		require(chancePercent in 0..100) { "chancePercent must be between 0 and 100" }
		require(requiredWeather != BattleWeather.NONE) { "requiredWeather cannot be NONE" }
	}
}
