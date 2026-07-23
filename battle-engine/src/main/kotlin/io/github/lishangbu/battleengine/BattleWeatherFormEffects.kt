package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleState

/** 按当前天气同步所有上场成员的天气形态。 */
internal fun BattleState.synchronizeWeatherForms(actorIds: Set<String>? = null): BattleState {
	val candidates = sides.flatMap { it.activeActorIds }
		.filter { actorIds == null || it in actorIds }
	return candidates.fold(this) { current, actorId ->
		val actor = current.participant(actorId) ?: return@fold current
		val restore = actor.abilityEffects.filterIsInstance<BattleAbilityEffect.WeatherFormRestore>()
			.firstOrNull { it.weather == current.environment.weather }
		val restorePair = restore?.formPairs?.firstOrNull { pair ->
			actor.battleFormProfiles[pair.alternateFormCode]?.creatureId == actor.creatureId
		}
		val restoreTarget = restorePair?.let { actor.battleFormProfiles[it.baseFormCode] }
		if (restoreTarget != null) {
			return@fold current.replaceParticipant(actor.changeBattleForm(restoreTarget)).appendEvent(
				BattleEvent.FormChanged(current.turnNumber, actorId, actor.creatureId, restoreTarget.creatureId),
			)
		}
		val effect = actor.abilityEffects.filterIsInstance<BattleAbilityEffect.WeatherFormChange>().firstOrNull()
			?: return@fold current
		val targetCode = effect.formCodesByWeather[current.environment.weather] ?: effect.defaultFormCode
		val target = actor.battleFormProfiles[targetCode] ?: return@fold current
		if (target.creatureId == actor.creatureId) return@fold current
		current.replaceParticipant(actor.changeBattleForm(target)).appendEvent(
			BattleEvent.FormChanged(current.turnNumber, actorId, actor.creatureId, target.creatureId),
		)
	}
}
