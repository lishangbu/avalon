package io.github.lishangbu.common.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class FixedWindowRateLimiterTests {
	@Test
	fun `limits each key and opens a fresh window after expiry`() {
		var now = 0L
		val limiter = FixedWindowRateLimiter(2, Duration.ofSeconds(1)) { now }

		assertThat(limiter.tryAcquire("client-a")).isTrue()
		assertThat(limiter.tryAcquire("client-a")).isTrue()
		assertThat(limiter.tryAcquire("client-a")).isFalse()
		assertThat(limiter.tryAcquire("client-b")).isTrue()

		now = Duration.ofSeconds(1).toNanos()
		assertThat(limiter.tryAcquire("client-a")).isTrue()
	}
}
