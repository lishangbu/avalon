package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleWeather

/** 返回成员个人技能与伤害结算应读取的天气。 */
internal fun BattleParticipant.effectiveWeather(weather: BattleWeather): BattleWeather =
	if (
		weather in setOf(BattleWeather.SUN, BattleWeather.RAIN) &&
		itemEffects.any { it is BattleItemEffect.SunRainEffectImmunity }
	) {
		BattleWeather.NONE
	} else {
		weather
	}
