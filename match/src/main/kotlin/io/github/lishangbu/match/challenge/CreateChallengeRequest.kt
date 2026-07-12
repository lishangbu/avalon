package io.github.lishangbu.match.challenge

/** 发起私人 Challenge 的幂等命令；目标只允许完整 displayName。 */
data class CreateChallengeRequest(
	var commandId: String = "",
	var challengedDisplayName: String = "",
	var leadPosition: Int = 0,
)
