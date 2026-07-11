package io.github.lishangbu.battlesession

import io.github.lishangbu.battleengine.model.BattleAction

data class TurnRequirements(
	val selections: List<TurnSelectionRequirement>,
)

data class TurnSelectionRequirement(
	val actorId: String,
	val options: List<BattleAction>,
) {
	init {
		require(actorId.isNotBlank()) { "actorId must not be blank" }
		require(options.isNotEmpty()) { "turn selection options must not be empty" }
		require(options.all { it.actorId == actorId }) { "all options must belong to the required actor" }
	}
}
