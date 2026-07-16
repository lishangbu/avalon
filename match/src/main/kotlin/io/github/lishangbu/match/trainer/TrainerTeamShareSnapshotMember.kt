package io.github.lishangbu.match.trainer

/** 分享快照中的完整 Team 成员，导入时必须重新通过当前资料合法性校验。 */
data class TrainerTeamShareSnapshotMember(
	var creatureId: Long = 0,
	var skinId: Long = 0,
	var skillIds: List<Long> = emptyList(),
	var abilityId: Long = 0,
	var itemId: Long = 0,
	var natureId: Long = 0,
	var teraElementId: Long = 0,
	var individualValues: Map<String, Int> = emptyMap(),
	var effortValues: Map<String, Int> = emptyMap(),
)
