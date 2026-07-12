package io.github.lishangbu.match.challenge

/** 冻结后的出战顺序、Lead 与完整成员数据；所有成员等级固定为 50。 */
data class TrainerTeamSnapshotRoster(
	var leadPosition: Int = 0,
	var members: List<TrainerTeamSnapshotMember> = emptyList(),
)
