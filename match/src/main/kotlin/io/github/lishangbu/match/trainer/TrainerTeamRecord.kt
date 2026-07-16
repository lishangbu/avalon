package io.github.lishangbu.match.trainer

/** Team 聚合根与有序成员组成的内部只读快照。 */
data class TrainerTeamRecord(
	val id: Long,
	val trainerId: Long,
	val name: String,
	val active: Boolean,
	val revision: Long,
	val members: List<TrainerTeamMemberRecord>,
)
