package io.github.lishangbu.match.challenge

/** Challenge 状态命令的乐观锁版本。 */
data class ChallengeRevisionRequest(var expectedRevision: Long = -1)
