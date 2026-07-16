package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleWeather

/** 判断场上是否存在压制全部天气效果的可战斗成员。 */
internal fun BattleState.weatherEffectsSuppressed(): Boolean =
	sides.flatMap { it.activeParticipants() }.any { participant ->
		participant.canBattle() && participant.abilityEffects.any { it is BattleAbilityEffect.WeatherEffectSuppression }
	}

/** 返回指定成员实际感受到的天气，同时合并全场压制和个人万能伞。 */
internal fun BattleState.effectiveWeatherFor(participant: BattleParticipant): BattleWeather =
	if (weatherEffectsSuppressed()) BattleWeather.NONE else participant.effectiveWeather(environment.weather)

/** 为伤害公式构造不改变真实持续时间的有效环境视图。 */
internal fun BattleState.effectiveEnvironmentFor(participant: BattleParticipant): BattleEnvironment =
	environment.copy(weather = effectiveWeatherFor(participant))
