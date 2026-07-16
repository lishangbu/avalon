package io.github.lishangbu.match.trainer

/** 已规范化并持久化的 Team Member 快照。 */
data class TrainerTeamMemberRecord(
	val creatureId: Long,
	val skinId: Long,
	val skillIds: List<Long>,
	val abilityId: Long,
	val itemId: Long,
	val natureId: Long,
	val teraElementId: Long,
	val individualValues: Map<String, Int>,
	val effortValues: Map<String, Int>,
)
