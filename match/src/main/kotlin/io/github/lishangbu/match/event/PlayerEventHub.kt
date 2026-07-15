package io.github.lishangbu.match.event

import io.github.lishangbu.match.trainer.TrainerSessionRegistry
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** 单节点连接注册表；认证超时、心跳超时和 Session 失效都会主动清除连接。 */
@Component
class PlayerEventHub(
	private val objectMapper: ObjectMapper,
	private val trainerSessions: TrainerSessionRegistry,
	private val metrics: PlayerEventMetrics,
	private val clock: Clock = Clock.systemUTC(),
) {
	private class Connection(
		val accountId: Long,
		val trainerId: Long,
		val credential: String,
		val session: WebSocketSession,
		@Volatile var lastHeartbeatAt: Instant,
	) {
		val mailbox = LinkedHashMap<Pair<String, String?>, Delivery>()
		var draining = false
	}

	private data class Delivery(val event: PlayerEvent, val closeAfter: CloseStatus? = null)

	private val authenticationDeadlines = ConcurrentHashMap<WebSocketSession, Instant>()
	private val connections = ConcurrentHashMap<WebSocketSession, Connection>()
	private val sessionsByTrainer = ConcurrentHashMap<Long, MutableSet<WebSocketSession>>()
	private val deliveryExecutor: ExecutorService = Executors.newCachedThreadPool { action ->
		Thread(action, "player-event-delivery").apply { isDaemon = true }
	}
	private val cleanup = Executors.newSingleThreadScheduledExecutor { action ->
		Thread(action, "player-event-cleanup").apply { isDaemon = true }
	}.also { it.scheduleAtFixedRate(::evictExpired, 5, 5, TimeUnit.SECONDS) }

	fun connected(session: WebSocketSession, now: Instant) {
		authenticationDeadlines[session] = now.plus(AUTHENTICATION_TIMEOUT)
		metrics.connected()
	}

	fun register(accountId: Long, trainerId: Long, credential: String, session: WebSocketSession, now: Instant, reconnect: Boolean) {
		authenticationDeadlines.remove(session)
		connections[session] = Connection(accountId, trainerId, credential, session, now)
		sessionsByTrainer.computeIfAbsent(trainerId) { ConcurrentHashMap.newKeySet() }.add(session)
		metrics.authenticated(reconnect)
	}

	fun authenticationFailed() = metrics.authenticationFailed()

	fun heartbeat(accountId: Long, credential: String, session: WebSocketSession, now: Instant): Boolean {
		val connection = connections[session] ?: return false
		if (connection.accountId != accountId || connection.credential != credential) return false
		if (trainerSessions.heartbeat(accountId, credential, now) == null) return false
		connection.lastHeartbeatAt = now
		return true
	}

	fun unregister(session: WebSocketSession, reason: String = "client") {
		val pending = authenticationDeadlines.remove(session) != null
		val connection = connections.remove(session)
		if (connection == null) {
			if (pending) metrics.disconnected(reason)
			return
		}
		synchronized(connection) {
			connection.mailbox.clear()
		}
		sessionsByTrainer[connection.trainerId]?.let { sessions ->
			sessions.remove(session)
			if (sessions.isEmpty()) sessionsByTrainer.remove(connection.trainerId, sessions)
		}
		metrics.disconnected(reason)
	}

	fun publish(trainerIds: Collection<Long>, event: PlayerEvent) {
		trainerIds.distinct().forEach { trainerId ->
			sessionsByTrainer[trainerId]?.toList()?.forEach { session ->
				val connection = connections[session]
				if (connection == null) return@forEach
				invalidReason(connection, Instant.now(clock))?.let { reason ->
					if (reason == "heartbeat.timeout") metrics.heartbeatTimedOut()
					closeAndUnregister(session, CloseStatus.POLICY_VIOLATION.withReason(reason))
					return@forEach
				}
				if (!enqueue(connection, Delivery(event))) slowConsumer(connection)
			}
		}
	}

	fun sendControl(session: WebSocketSession, event: PlayerEvent) {
		connections[session]?.let { connection ->
			if (!enqueue(connection, Delivery(event))) slowConsumer(connection)
		}
	}

	/** 安全事件撤销账户下全部实时连接，先告知客户端清理凭据，再阻止其继续接收领域事件。 */
	fun revokeAccount(accountId: Long) {
		connections.values.filter { it.accountId == accountId }.forEach { connection ->
			enqueue(
				connection,
				Delivery(
					PlayerEvent("SESSION_REVOKED", null, null),
					CloseStatus.POLICY_VIOLATION.withReason("session.revoked"),
				),
				replaceQueue = true,
			)
		}
	}

	@PreDestroy
	fun shutdown() {
		cleanup.shutdownNow()
		(connections.keys + authenticationDeadlines.keys).forEach { closeAndUnregister(it, CloseStatus.GOING_AWAY) }
		deliveryExecutor.shutdownNow()
	}

	internal fun evictExpired(now: Instant = Instant.now(clock)) {
		authenticationDeadlines.entries.filter { (_, deadline) -> !now.isBefore(deadline) }
			.forEach { (session) ->
				metrics.authenticationTimedOut()
				closeAndUnregister(session, CloseStatus.POLICY_VIOLATION.withReason("authentication.timeout"))
			}
		connections.values.mapNotNull { connection -> invalidReason(connection, now)?.let { connection to it } }
			.forEach { (connection, reason) ->
				if (reason == "heartbeat.timeout") metrics.heartbeatTimedOut()
				closeAndUnregister(connection.session, CloseStatus.POLICY_VIOLATION.withReason(reason))
			}
	}

	private fun invalidReason(connection: Connection, now: Instant): String? = when {
		!connection.session.isOpen -> "client"
		!now.isBefore(connection.lastHeartbeatAt.plus(HEARTBEAT_TIMEOUT)) -> "heartbeat.timeout"
		trainerSessions.validate(connection.accountId, connection.credential, now) == null -> "trainer-session.invalid"
		else -> null
	}

	private fun closeAndUnregister(session: WebSocketSession, status: CloseStatus) {
		unregister(session, status.reason?.takeIf(String::isNotBlank) ?: "server")
		if (session.isOpen) runCatching { session.close(status) }
	}

	private fun enqueue(connection: Connection, delivery: Delivery, replaceQueue: Boolean = false): Boolean {
		var startDrain = false
		synchronized(connection) {
			if (connections[connection.session] !== connection) return false
			if (replaceQueue) connection.mailbox.clear()
			val key = delivery.event.type to delivery.event.resourceId
			val current = connection.mailbox[key]
			if (current != null) {
				val currentRevision = current.event.revision
				val nextRevision = delivery.event.revision
				if (currentRevision != null && nextRevision != null && nextRevision <= currentRevision) return true
				connection.mailbox[key] = delivery
			} else {
				if (connection.mailbox.size >= MAILBOX_CAPACITY) return false
				connection.mailbox[key] = delivery
			}
			if (!connection.draining) {
				connection.draining = true
				startDrain = true
			}
		}
		if (startDrain) deliveryExecutor.execute { drain(connection) }
		return true
	}

	private fun drain(connection: Connection) {
		while (true) {
			val delivery = synchronized(connection) {
				val first = connection.mailbox.entries.firstOrNull()
				if (first == null) {
					connection.draining = false
					return
				}
				connection.mailbox.remove(first.key)
				first.value
			}
			val sent = runCatching {
				connection.session.sendMessage(TextMessage(objectMapper.writeValueAsString(delivery.event)))
			}.isSuccess
			if (!sent) {
				metrics.deliveryFailed(delivery.event.type)
				closeAndUnregister(connection.session, CloseStatus.SERVER_ERROR)
				return
			}
			metrics.delivered(delivery.event.type)
			delivery.closeAfter?.let {
				closeAndUnregister(connection.session, it)
				return
			}
		}
	}

	private fun slowConsumer(connection: Connection) {
		metrics.slowConsumer()
		closeAndUnregister(connection.session, CloseStatus(1013, "slow-consumer"))
	}

	private companion object {
		val AUTHENTICATION_TIMEOUT: Duration = Duration.ofSeconds(10)
		val HEARTBEAT_TIMEOUT: Duration = Duration.ofSeconds(35)
		const val MAILBOX_CAPACITY = 256
	}
}
