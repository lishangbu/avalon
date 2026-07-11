package io.github.lishangbu.battlesession.model

import java.time.Duration

/**
 * 限制单个 Runtime 的活跃会话数量、Recent Session 数量与保留时间。
 *
 * 活跃容量耗尽时拒绝创建，不通过淘汰运行中会话释放名额。
 */
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
