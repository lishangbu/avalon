package io.github.lishangbu.battlerules

import io.github.lishangbu.battlesession.BattleSessionRuntime
import io.github.lishangbu.battlesession.SessionRuntimeCapacity
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** 进程内 Battle Session Runtime 的 Spring 装配入口。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BattleSessionRuntimeProperties::class)
class BattleSessionConfig {
	@Bean
	fun battleSessionRuntime(properties: BattleSessionRuntimeProperties): BattleSessionRuntime =
		BattleSessionRuntime(
			capacity = SessionRuntimeCapacity(
				maxActiveSessions = properties.maxActiveSessions,
				maxRecentSessions = properties.maxRecentSessions,
				recentSessionTtl = properties.recentSessionTtl,
				retryAfter = properties.retryAfter,
			),
		)
}
