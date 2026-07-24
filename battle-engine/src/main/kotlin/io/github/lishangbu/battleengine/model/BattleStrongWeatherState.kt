package io.github.lishangbu.battleengine.model

/** 记录强天气及当前实际维持该天气的上场成员。 */
data class BattleStrongWeatherState(
	val weather: BattleStrongWeather,
	val sourceActorId: String,
) {
	init {
		require(sourceActorId.isNotBlank()) { "sourceActorId must not be blank" }
	}
}
