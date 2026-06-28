package io.github.lishangbu.battleengine.model

/**
 * 战斗中的全场环境快照。
 *
 * 环境是战斗运行态的一部分，描述当前天气和场地及其剩余回合。剩余回合允许为空，表示该环境来自永久规则
 * 或测试 fixture 暂不关心持续时间。状态机会在回合末统一扣减非空计数，耗尽时恢复为无天气/无场地并产生结束事件。
 */
data class BattleEnvironment(
	val weather: BattleWeather = BattleWeather.NONE,
	val weatherTurnsRemaining: Int? = null,
	val terrain: BattleTerrain = BattleTerrain.NONE,
	val terrainTurnsRemaining: Int? = null,
) {
	init {
		require(weatherTurnsRemaining == null || weatherTurnsRemaining > 0) {
			"weatherTurnsRemaining must be positive when present"
		}
		require(terrainTurnsRemaining == null || terrainTurnsRemaining > 0) {
			"terrainTurnsRemaining must be positive when present"
		}
	}
}
