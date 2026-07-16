package io.github.lishangbu.match.trainer

/** 玩家可复制给另一名玩家的 Team 分享凭证。 */
data class TrainerTeamShareResponse(
	val code: String,
	val teamRevision: Long,
)
