package io.github.lishangbu.battleengine.model

/**
 * 技能命中后尝试施加到某一侧场上的速度修正效果。
 *
 * 这是技能规则到一侧场上状态的结构化桥接对象。它与 [BattleSideSpeedModifier] 拆开，是因为同一个速度修正
 * 可以由不同技能、不同触发时机或不同前置条件建立；application 描述“这次技能如何尝试建立效果”，modifier
 * 描述“效果建立后在战斗状态里如何参与行动排序”。
 *
 * `targetSide` 使用引擎枚举表达使用者侧或目标侧，避免纯引擎解析数据库中的字符串。
 * `requiredWeather` 预留给未来需要天气前置条件的速度类场上效果；若为空则没有天气限制。
 */
data class BattleSideSpeedModifierApplication(
	val targetSide: BattleSideConditionTarget,
	val speedModifier: BattleSideSpeedModifier,
	val chancePercent: Int = 100,
	val requiredWeather: BattleWeather? = null,
) {
	init {
		require(chancePercent in 0..100) { "chancePercent must be between 0 and 100" }
		require(requiredWeather != BattleWeather.NONE) { "requiredWeather cannot be NONE" }
	}
}
