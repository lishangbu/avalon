package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleStat

/** 返回驱劲能量运行时标记对指定能力的倍率。 */
internal fun BattleParticipant.boosterEnergyMultiplier(stat: BattleStat, environment: BattleEnvironment): Double {
	val environmentBoostActive = abilityEffects
		.filterIsInstance<BattleAbilityEffect.EnvironmentHighestStatMultiplier>()
		.any { effect ->
			(effect.requiredWeather != null && effect.requiredWeather == environment.weather) ||
				(effect.requiredTerrain != null && effect.requiredTerrain == environment.terrain)
		}
	val boostedStat = if (environmentBoostActive) highestRawBattleStat() else boosterEnergyStat
	return if (boostedStat != stat) 1.0 else if (stat == BattleStat.SPEED) 1.5 else 1.3
}

/** 按攻击、防御、特攻、特防、速度的稳定顺序返回原始数值最高项。 */
private fun BattleParticipant.highestRawBattleStat(): BattleStat =
	listOf(
		BattleStat.ATTACK to attack,
		BattleStat.DEFENSE to defense,
		BattleStat.SPECIAL_ATTACK to specialAttack,
		BattleStat.SPECIAL_DEFENSE to specialDefense,
		BattleStat.SPEED to speed,
	).maxBy { it.second }.first
