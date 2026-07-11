package io.github.lishangbu.battlesession

/**
 * 为资料装配层分配会话内稳定标识。
 *
 * 该类型只读取阵容成员数量，不理解资料 DTO；调用方使用返回的布局装配 [BattleInitialState]。
 */
class SessionRosterIdentifiers {
	fun assign(
		sides: List<SessionRosterSideInput>,
		activeParticipantsPerSide: Int,
	): SessionRosterLayout {
		require(sides.size == 2) { "exactly two roster sides are required" }
		require(activeParticipantsPerSide > 0) { "activeParticipantsPerSide must be positive" }
		require(sides.all { it.activeParticipantIndexes.size == activeParticipantsPerSide }) {
			"active participant index count must match activeParticipantsPerSide"
		}

		return SessionRosterLayout(
			sides = sides.mapIndexed { sideIndex, side ->
				val sideId = "side-${sideIndex + 1}"
				val actorIds = (1..side.participantCount).map { participantIndex ->
					"$sideId-actor-$participantIndex"
				}
				SessionRosterSideLayout(
					sideId = sideId,
					actorIds = actorIds,
					activeActorIds = side.activeParticipantIndexes.map(actorIds::get),
				)
			},
		)
	}
}

data class SessionRosterSideInput(
	val participantCount: Int,
	val activeParticipantIndexes: List<Int>,
) {
	init {
		require(participantCount > 0) { "participantCount must be positive" }
		require(activeParticipantIndexes.isNotEmpty()) { "activeParticipantIndexes must not be empty" }
		require(activeParticipantIndexes.toSet().size == activeParticipantIndexes.size) {
			"activeParticipantIndexes must not contain duplicates"
		}
		require(activeParticipantIndexes.all { it in 0 until participantCount }) {
			"activeParticipantIndexes must reference existing participants"
		}
	}
}

data class SessionRosterLayout(
	val sides: List<SessionRosterSideLayout>,
)

data class SessionRosterSideLayout(
	val sideId: String,
	val actorIds: List<String>,
	val activeActorIds: List<String>,
)
