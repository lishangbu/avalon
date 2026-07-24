package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleState

/** 按当前场地同步场上成员的临时属性身份，并在场地消失时恢复原始属性。 */
internal fun BattleState.synchronizeTerrainElementIdentities(actorIds: Set<String>? = null): BattleState {
	val candidates = sides.flatMap { it.activeParticipants() }
		.filter { actorIds == null || it.actorId in actorIds }
	return candidates.fold(this) { current, snapshot ->
		val participant = current.participant(snapshot.actorId) ?: return@fold current
		if (participant.terastallized) return@fold current
		val effect = participant.allAbilityEffects().filterIsInstance<BattleAbilityEffect.TerrainElementIdentity>()
			.firstOrNull() ?: return@fold current
		val terrainElementId = if (effect in participant.abilityEffects) {
			effect.elementIdsByTerrain[current.environment.terrain]
		} else {
			null
		}
		val nextElementIds = terrainElementId?.let(::setOf) ?: participant.originalElementIds
		if (participant.elementIds == nextElementIds) return@fold current
		current.replaceParticipant(participant.copy(elementIds = nextElementIds)).appendEvent(
			BattleEvent.TerrainElementIdentityChanged(
				current.turnNumber,
				participant.actorId,
				current.environment.terrain,
				participant.elementIds,
				nextElementIds,
			),
		)
	}
}
