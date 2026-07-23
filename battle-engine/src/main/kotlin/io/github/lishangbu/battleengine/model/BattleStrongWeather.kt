package io.github.lishangbu.battleengine.model

/** 只能由同级强天气覆盖，并在最后一个来源离场后结束的全场天气。 */
enum class BattleStrongWeather(val effectiveWeather: BattleWeather) {
	HARSH_SUNLIGHT(BattleWeather.SUN),
	HEAVY_RAIN(BattleWeather.RAIN),
	STRONG_WINDS(BattleWeather.NONE),
}
