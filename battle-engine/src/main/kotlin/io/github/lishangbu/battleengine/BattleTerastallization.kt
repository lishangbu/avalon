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
	if (actor.abilityEffects.none { it is BattleAbilityEffect.TerastallizationEnvironmentClear }) {
		return afterTerastallization
	}
	val clearEvents = buildList {
		if (environment.weather != BattleWeather.NONE) {
			add(BattleEvent.WeatherEnded(turnNumber, environment.weather))
		}
		if (environment.terrain != BattleTerrain.NONE) {
			add(BattleEvent.TerrainEnded(turnNumber, environment.terrain, actor.actorId))
		}
	}
	return afterTerastallization.copy(
		environment = environment.copy(
			weather = BattleWeather.NONE,
			weatherTurnsRemaining = null,
			terrain = BattleTerrain.NONE,
			terrainTurnsRemaining = null,
		),
	).appendEvents(clearEvents).synchronizeTerrainElementIdentities()
}
