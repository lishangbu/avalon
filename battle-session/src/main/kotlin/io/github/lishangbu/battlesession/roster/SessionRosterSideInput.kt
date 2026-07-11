package io.github.lishangbu.battlesession.roster

/** 描述一侧阵容规模及创建时上场成员位置，不携带调用方自定义场内标识。 */
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
