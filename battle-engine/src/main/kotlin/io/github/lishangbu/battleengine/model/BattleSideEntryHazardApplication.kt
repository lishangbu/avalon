package io.github.lishangbu.battleengine.model

/**
 * 技能命中后尝试建立的一侧入场陷阱效果。
 *
 * 这是技能规则到战斗侧状态的结构化桥接对象。它描述“这次技能命中后应该把哪一种入场陷阱放到哪一侧”，
 * 而 [BattleSideEntryHazard] 描述“陷阱写入后在一侧场上的稳定状态”。两者拆开后，同一种陷阱可以由不同技能
 * 或不同前置条件建立，而换入触发逻辑仍只读取一侧上的陷阱状态。
 *
 * `targetSide` 使用引擎枚举表达使用者侧或目标侧，避免纯引擎解析数据库字符串。`requiredWeather` 预留给
 * 未来可能存在的天气前置条件；当前现代主系列常见入场陷阱没有天气要求。
 */
data class BattleSideEntryHazardApplication(
	val targetSide: BattleSideConditionTarget,
	val hazard: BattleSideEntryHazard,
	val chancePercent: Int = 100,
	val requiredWeather: BattleWeather? = null,
) {
	init {
		require(chancePercent in 0..100) { "chancePercent must be between 0 and 100" }
		require(requiredWeather != BattleWeather.NONE) { "requiredWeather cannot be NONE" }
	}
}
