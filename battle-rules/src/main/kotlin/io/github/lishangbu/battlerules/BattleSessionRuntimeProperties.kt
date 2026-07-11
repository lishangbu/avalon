package io.github.lishangbu.battlerules

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/** 单个进程内 Session Runtime 的容量与 Recent Session 保留配置。 */
@ConfigurationProperties("backend.battle-session.runtime")
data class BattleSessionRuntimeProperties(
	val maxActiveSessions: Int = 1_000,
	val maxRecentSessions: Int = 1_000,
	val recentSessionTtl: Duration = Duration.ofMinutes(15),
	val retryAfter: Duration = Duration.ofSeconds(1),
)
