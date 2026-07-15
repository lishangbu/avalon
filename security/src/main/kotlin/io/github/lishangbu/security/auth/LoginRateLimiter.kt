package io.github.lishangbu.security.auth

import io.github.lishangbu.common.web.FixedWindowRateLimiter
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class LoginRateLimiter {
	private val delegate = FixedWindowRateLimiter(60, Duration.ofMinutes(1))

	fun tryAcquire(remoteAddress: String, username: String): Boolean =
		delegate.tryAcquire("$remoteAddress:${username.trim().lowercase()}")
}
