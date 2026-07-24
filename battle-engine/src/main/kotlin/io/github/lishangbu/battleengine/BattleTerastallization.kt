package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather

/** 在技能实际尝试前消耗一方整场唯一的太晶化机会。 */
internal fun BattleState.applyTerastallization(action: BattleAction.UseSkill): BattleState {
	if (!action.terastallize) return this
	val side = requireNotNull(sideOf(action.actorId)) { "tera actor side is missing" }
	require(!side.terastallizationUsed) { "side has already used terastallization" }
	val actor = requireNotNull(side.participant(action.actorId)) { "tera actor is missing" }
	val teraElementId = requireNotNull(actor.teraElementId) { "actor has no tera element" }
	require(!actor.terastallized) { "actor has already terastallized" }
	val changedActor = actor.copy(elementIds = setOf(teraElementId), terastallized = true)
	val changedSide = side.replaceParticipant(changedActor).copy(terastallizationUsed = true)
	val afterTerastallization = copy(sides = sides.map { if (it.sideId == side.sideId) changedSide else it })
		.appendEvent(BattleEvent.ParticipantTerastallized(turnNumber, actor.actorId, teraElementId))
		.applyTerastallizationStatStageChanges(actor.actorId, actor.abilityEffects)
	if (actor.abilityEffects.none { it is BattleAbilityEffect.TerastallizationEnvironmentClear }) {
		return afterTerastallization
	}
	val clearEvents = buildList {
		if (afterTerastallization.environment.weather != BattleWeather.NONE) {
			add(BattleEvent.WeatherEnded(turnNumber, afterTerastallization.environment.weather))
		}
		if (afterTerastallization.environment.terrain != BattleTerrain.NONE) {
			add(BattleEvent.TerrainEnded(turnNumber, afterTerastallization.environment.terrain, actor.actorId))
		}
	}
	return afterTerastallization.copy(
		environment = afterTerastallization.environment.copy(
			weather = BattleWeather.NONE,
			weatherTurnsRemaining = null,
			terrain = BattleTerrain.NONE,
			terrainTurnsRemaining = null,
		),
	).appendEvents(clearEvents).synchronizeTerrainElementIdentities().synchronizeWeatherForms()
}

/** 应用由太晶化触发的自身能力阶级变化，并只记录实际发生的变化。 */
private fun BattleState.applyTerastallizationStatStageChanges(
	actorId: String,
	effects: List<BattleAbilityEffect>,
): BattleState = effects.filterIsInstance<BattleAbilityEffect.TerastallizationStatStageChange>()
	.fold(this) { current, effect ->
		val participant = current.participant(actorId) ?: return@fold current
		val before = participant.statStage(effect.stat)
		val changed = participant.changeStatStage(effect.stat, effect.stageDelta)
		val actualDelta = changed.statStage(effect.stat) - before
		if (actualDelta == 0) {
			current
		} else {
			current.replaceParticipant(changed).appendEvent(
				BattleEvent.StatStageChanged(
					turnNumber = current.turnNumber,
					actorId = actorId,
					targetActorId = actorId,
					stat = effect.stat,
					delta = actualDelta,
					currentStage = changed.statStage(effect.stat),
				),
			)
		}
	}
