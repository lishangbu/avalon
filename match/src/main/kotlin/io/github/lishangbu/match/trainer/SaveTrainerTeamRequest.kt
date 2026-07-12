package io.github.lishangbu.match.trainer

/** Team 的完整替换命令；首次创建时 expectedRevision 为空。 */
data class SaveTrainerTeamRequest(
	var expectedRevision: Long? = null,
	var members: List<SaveTrainerTeamMemberRequest> = emptyList(),
)
