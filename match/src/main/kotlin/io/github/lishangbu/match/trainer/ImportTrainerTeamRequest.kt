package io.github.lishangbu.match.trainer

/** 从不可枚举分享短码导入独立 Team 副本的命令。 */
data class ImportTrainerTeamRequest(
	var shareCode: String = "",
	var name: String? = null,
)
