package io.github.lishangbu.battlerules

import io.github.lishangbu.battlesession.BattleSessionRuntime
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** 验证 Session Runtime 容量配置的默认值、显式绑定与 Spring 装配。 */
class BattleSessionConfigTests {
	private val contextRunner = ApplicationContextRunner()
		.withUserConfiguration(BattleSessionConfig::class.java)

	@Test
	fun `runtime capacity properties keep designed defaults`() {
		contextRunner.run { context ->
			val properties = context.getBean(BattleSessionRuntimeProperties::class.java)

			assertEquals(1_000, properties.maxActiveSessions)
			assertEquals(1_000, properties.maxRecentSessions)
			assertEquals(Duration.ofMinutes(15), properties.recentSessionTtl)
			assertEquals(Duration.ofSeconds(1), properties.retryAfter)
			assertNotNull(context.getBean(BattleSessionRuntime::class.java))
		}
	}

	@Test
	fun `runtime capacity properties bind explicit deployment values`() {
		contextRunner
			.withPropertyValues(
				"backend.battle-session.runtime.max-active-sessions=17",
				"backend.battle-session.runtime.max-recent-sessions=23",
				"backend.battle-session.runtime.recent-session-ttl=2m",
				"backend.battle-session.runtime.retry-after=7s",
			)
			.run { context ->
				val properties = context.getBean(BattleSessionRuntimeProperties::class.java)

				assertEquals(17, properties.maxActiveSessions)
				assertEquals(23, properties.maxRecentSessions)
				assertEquals(Duration.ofMinutes(2), properties.recentSessionTtl)
				assertEquals(Duration.ofSeconds(7), properties.retryAfter)
			}
	}
}
