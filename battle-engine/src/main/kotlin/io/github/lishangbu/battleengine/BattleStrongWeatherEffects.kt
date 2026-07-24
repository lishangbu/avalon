package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleStrongWeather
import io.github.lishangbu.battleengine.model.BattleStrongWeatherState
import io.github.lishangbu.battleengine.model.BattleWeather

/** 建立强天气，并记录当前实际维持它的上场成员。 */
internal fun BattleState.startStrongWeather(actorId: String, weather: BattleStrongWeather): BattleState {
	if (environment.strongWeather == weather && environment.strongWeatherSourceActorId == actorId) return this
	val changed = copy(
		environment = environment.copy(
			weather = BattleWeather.NONE,
			weatherTurnsRemaining = null,
			strongWeatherState = BattleStrongWeatherState(weather, actorId),
		),
	)
	val withEvent = if (weather.effectiveWeather == BattleWeather.NONE) changed else changed.appendEvent(
		BattleEvent.WeatherStarted(turnNumber, actorId, weather.effectiveWeather, null),
	)
	return withEvent.synchronizeWeatherForms()
}

/** 在来源离场或倒下后由其它强天气持有者接管，否则结束强天气。 */
internal fun BattleState.synchronizeStrongWeather(): BattleState {
	val currentWeather = environment.strongWeather ?: return this
	val currentSource = environment.strongWeatherSourceActorId
	val holders = sides.flatMap { it.activeParticipants() }
		.filter { it.canBattle() }
		.flatMap { participant ->
			participant.abilityEffects.filterIsInstance<BattleAbilityEffect.SwitchInStrongWeatherChange>()
				.map { participant.actorId to it.weather }
		}
	if (holders.any { it.first == currentSource && it.second == currentWeather }) return this
	val replacement = holders.firstOrNull()
	if (replacement != null) return startStrongWeather(replacement.first, replacement.second)
	val cleared = copy(
		environment = environment.copy(
			strongWeatherState = null,
		),
	)
	val withEvent = if (currentWeather.effectiveWeather == BattleWeather.NONE) cleared else cleared.appendEvent(
		BattleEvent.WeatherEnded(turnNumber, currentWeather.effectiveWeather),
	)
	return withEvent.synchronizeWeatherForms()
}

/** 判断当前未被压制的强天气是否会令伤害技能直接失败。 */
internal fun BattleState.skillBlockedByStrongWeather(
	actor: BattleParticipant,
	skill: BattleSkillSlot,
): Boolean {
	if (skill.damageClass == BattleDamageClass.STATUS || skill.typelessDamage || weatherEffectsSuppressed()) return false
	val skillElementId = skill.effectiveElementId(effectiveWeatherFor(actor), environment.terrain, actor)
	return when (environment.strongWeather) {
		BattleStrongWeather.HARSH_SUNLIGHT -> skillElementId == rules.elementId("water")
		BattleStrongWeather.HEAVY_RAIN -> skillElementId == rules.elementId("fire")
		BattleStrongWeather.STRONG_WINDS, null -> false
	}
}
