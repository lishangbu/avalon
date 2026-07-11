package io.github.lishangbu.battlesession.roster

/**
 * 为资料装配层分配会话内稳定标识。
 *
 * 该类型只读取阵容成员数量，不理解资料 DTO；调用方使用返回的布局装配战斗初始状态。
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
