package io.github.lishangbu.battlesession.roster

/** 保存一侧阵容的 sideId、成员 actorId 与初始上场 actorId。 */
data class SessionRosterSideLayout(
	val sideId: String,
	val actorIds: List<String>,
	val activeActorIds: List<String>,
)
