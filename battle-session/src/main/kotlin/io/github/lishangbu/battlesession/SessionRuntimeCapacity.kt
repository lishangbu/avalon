package io.github.lishangbu.battlesession

import java.time.Duration

data class SessionRuntimeCapacity(
	val maxActiveSessions: Int = 1_000,
	val maxRecentSessions: Int = 1_000,
	val recentSessionTtl: Duration = Duration.ofMinutes(15),
	val retryAfter: Duration = Duration.ofSeconds(1),
) {
	init {
		require(maxActiveSessions > 0) { "maxActiveSessions must be positive" }
		require(maxRecentSessions >= 0) { "maxRecentSessions must not be negative" }
		require(!recentSessionTtl.isNegative && !recentSessionTtl.isZero) { "recentSessionTtl must be positive" }
		require(!retryAfter.isNegative && !retryAfter.isZero) { "retryAfter must be positive" }
	}
}
