package io.github.lishangbu.battlesession

import io.github.lishangbu.battleengine.BattleActionValidator
import io.github.lishangbu.battleengine.canBattle
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleState

class TurnRequirementsDeriver(
	private val validator: BattleActionValidator = BattleActionValidator(),
) {
	fun derive(state: BattleState): TurnRequirements {
		if (state.result != null) {
			return TurnRequirements(emptyList())
		}
		val selections = state.sides
			.flatMap { side -> side.activeParticipants() }
			.mapNotNull { actor -> requirementFor(state, actor) }
		return TurnRequirements(selections)
	}

	private fun requirementFor(state: BattleState, actor: BattleParticipant): TurnSelectionRequirement? {
		val switchOptions = switchOptions(state, actor)
		if (!actor.canBattle()) {
			return switchOptions.takeIf { it.isNotEmpty() }?.let { TurnSelectionRequirement(actor.actorId, it) }
		}
		if (actor.lockedMoveTurnsRemaining > 0 || actor.chargingTurnsRemaining > 0 || actor.rechargeTurnsRemaining > 0) {
			return null
		}
		if (validator.requiresStruggleFallback(actor)) {
			if (switchOptions.isEmpty()) {
				return null
			}
			val canonicalStruggleChoice = actor.skillSlots.firstOrNull()?.let { skill ->
				BattleAction.UseSkill(actor.actorId, skill.skillId, actor.actorId)
			}
			val options = listOfNotNull(canonicalStruggleChoice) + switchOptions
			return TurnSelectionRequirement(actor.actorId, options)
		}

		val options = skillOptions(state, actor) + switchOptions
		return options.takeIf { it.isNotEmpty() }?.let { TurnSelectionRequirement(actor.actorId, it) }
	}

	private fun skillOptions(state: BattleState, actor: BattleParticipant): List<BattleAction> {
		val activeTargets = state.sides
			.flatMap { side -> side.activeParticipants() }
			.filter { it.canBattle() }
		return actor.skillSlots.flatMap { skill ->
			val targetActorIds = if (skill.targetScope == BattleSkillTargetScope.SELECTED_TARGET) {
				activeTargets.map { it.actorId }
			} else {
				listOf(actor.actorId)
			}
			targetActorIds.map { targetActorId ->
				BattleAction.UseSkill(actor.actorId, skill.skillId, targetActorId)
			}
		}.filter { action -> validator.validate(state, listOf(action)).isEmpty() }
			.distinct()
	}

	private fun switchOptions(state: BattleState, actor: BattleParticipant): List<BattleAction> {
		val side = state.sideOf(actor.actorId) ?: return emptyList()
		return side.participants
			.filter { participant -> !side.isActive(participant.actorId) && participant.canBattle() }
			.map { participant -> BattleAction.SwitchParticipant(actor.actorId, participant.actorId) }
			.filter { action -> validator.validate(state, listOf(action)).isEmpty() }
	}
}
