package io.github.lishangbu.battlesession.roster

/** 保存 Runtime 为双方阵容分配的稳定会话内标识布局。 */
data class SessionRosterLayout(
	val sides: List<SessionRosterSideLayout>,
)
