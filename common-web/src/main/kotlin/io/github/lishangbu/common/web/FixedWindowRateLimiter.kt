package io.github.lishangbu.common.web

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/** Small single-node fixed-window limiter for protecting application entry points. */
class FixedWindowRateLimiter(
	private val maximumAttempts: Int,
	window: Duration,
	private val nanoTime: () -> Long = System::nanoTime,
) {
	private data class Window(val startedAt: Long, val attempts: Int)

	private val windowNanos = window.toNanos()
	private val windows = ConcurrentHashMap<String, Window>()

	fun tryAcquire(key: String): Boolean {
		val now = nanoTime()
		var allowed = false
		windows.compute(key) { _, current ->
			val active = current?.takeIf { now - it.startedAt < windowNanos }
			if (active == null) {
				allowed = true
				Window(now, 1)
			} else if (active.attempts < maximumAttempts) {
				allowed = true
				active.copy(attempts = active.attempts + 1)
			} else active
		}
		if (windows.size > MAXIMUM_KEYS) {
			windows.entries.removeIf { now - it.value.startedAt >= windowNanos }
		}
		return allowed
	}

	private companion object {
		const val MAXIMUM_KEYS = 10_000
	}
}
