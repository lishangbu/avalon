package io.github.lishangbu.battlesession.model

import io.github.lishangbu.battleengine.model.BattleAction

/** 限定一个场内成员必须从服务端派生选项中提交且仅提交一个行动。 */
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
