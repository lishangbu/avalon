package io.github.lishangbu.match.trainer

/** 与来源 Team 后续修改隔离的版本化分享文档。 */
data class TrainerTeamShareSnapshot(
	var schemaVersion: Int = 1,
	var name: String = "",
	var members: List<TrainerTeamShareSnapshotMember> = emptyList(),
)
