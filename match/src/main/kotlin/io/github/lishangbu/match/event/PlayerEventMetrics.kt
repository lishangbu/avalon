package io.github.lishangbu.match.event

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/** 玩家实时通道的低基数运行指标；标签不包含账户、Trainer 或资源标识。 */
@Component
class PlayerEventMetrics(private val registry: MeterRegistry) {
	private val activeConnections = AtomicInteger()
	private val deliveryFailures = ConcurrentLinkedQueue<Instant>()

	init {
		registry.gauge("avalon.player.events.connections.active", activeConnections)
	}

	fun authenticated(reconnect: Boolean) {
		counter("avalon.player.events.authentications", "result", "success", "connection", if (reconnect) "reconnect" else "initial").increment()
	}

	fun authenticationFailed() = counter("avalon.player.events.authentications", "result", "failure", "connection", "initial").increment()
	fun connected() = activeConnections.incrementAndGet()

	fun disconnected(reason: String) {
		activeConnections.updateAndGet { it.coerceAtLeast(1) - 1 }
		counter("avalon.player.events.disconnections", "reason", reason.toMetricReason()).increment()
	}

	fun delivered(type: String) = counter("avalon.player.events.deliveries", "event", type, "result", "success").increment()

	fun deliveryFailed(type: String, now: Instant = Instant.now()) {
		counter("avalon.player.events.deliveries", "event", type, "result", "failure").increment()
		deliveryFailures.add(now)
	}

	fun heartbeatTimedOut() = counter("avalon.player.events.timeouts", "phase", "heartbeat").increment()
	fun authenticationTimedOut() = counter("avalon.player.events.timeouts", "phase", "authentication").increment()

	/** 告警观察滑动窗口，避免一次历史故障让健康状态永久保持异常。 */
	fun recentDeliveryFailures(now: Instant = Instant.now(), window: Duration = ALERT_WINDOW): Int {
		val cutoff = now.minus(window)
		deliveryFailures.removeIf { it.isBefore(cutoff) }
		return deliveryFailures.size
	}

	private fun counter(name: String, vararg tags: String): Counter = registry.counter(name, *tags)

	private fun String.toMetricReason(): String = when (this) {
		"authentication.timeout", "heartbeat.timeout", "session.revoked", "trainer-session.invalid",
		"client", "server" -> this
		else -> "other"
	}

	companion object {
		val ALERT_WINDOW: Duration = Duration.ofMinutes(5)
		const val DELIVERY_FAILURE_ALERT_THRESHOLD = 5
	}
}
