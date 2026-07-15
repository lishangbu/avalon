package io.github.lishangbu.match.event

import io.github.lishangbu.common.web.FixedWindowRateLimiter
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import java.time.Duration

@Component
class PlayerEventConnectionRateLimiter {
	private val delegate = FixedWindowRateLimiter(20, Duration.ofMinutes(1))

	fun tryAcquire(session: WebSocketSession): Boolean =
		delegate.tryAcquire(session.remoteAddress?.address?.hostAddress ?: "unknown")
}
