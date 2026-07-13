package io.github.lishangbu.match.event

import io.github.lishangbu.match.trainer.TrainerSessionRegistry
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** 单节点连接注册表；认证超时、心跳超时和 Session 失效都会主动清除连接。 */
@Component
class PlayerEventHub(
	private val objectMapper: ObjectMapper,
	private val trainerSessions: TrainerSessionRegistry,
	private val metrics: PlayerEventMetrics,
) {
	private data class Connection(
		val accountId: Long,
		val trainerId: Long,
		val credential: String,
		val session: WebSocketSession,
		@Volatile var lastHeartbeatAt: Instant,
	)

	private val authenticationDeadlines = ConcurrentHashMap<WebSocketSession, Instant>()
	private val connections = ConcurrentHashMap<WebSocketSession, Connection>()
	private val sessionsByTrainer = ConcurrentHashMap<Long, MutableSet<WebSocketSession>>()
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
		sessionsByTrainer[connection.trainerId]?.let { sessions ->
			sessions.remove(session)
			if (sessions.isEmpty()) sessionsByTrainer.remove(connection.trainerId, sessions)
		}
		metrics.disconnected(reason)
	}

	fun publish(trainerIds: Collection<Long>, event: PlayerEvent) {
		val message = TextMessage(objectMapper.writeValueAsString(event))
		trainerIds.distinct().forEach { trainerId ->
			sessionsByTrainer[trainerId]?.toList()?.forEach { session ->
				val connection = connections[session]
				if (connection == null) return@forEach
				invalidReason(connection, Instant.now())?.let { reason ->
					if (reason == "heartbeat.timeout") metrics.heartbeatTimedOut()
					closeAndUnregister(session, CloseStatus.POLICY_VIOLATION.withReason(reason))
					return@forEach
				}
				val sent = runCatching { synchronized(session) { session.sendMessage(message) } }.isSuccess
				if (sent) metrics.delivered(event.type)
				else {
					metrics.deliveryFailed(event.type)
					closeAndUnregister(session, CloseStatus.SERVER_ERROR)
				}
			}
		}
	}

	/** 安全事件撤销账户下全部实时连接，先告知客户端清理凭据，再阻止其继续接收领域事件。 */
	fun revokeAccount(accountId: Long) {
		connections.values.filter { it.accountId == accountId }.forEach { connection ->
			val sent = runCatching { synchronized(connection.session) {
				connection.session.sendMessage(TextMessage(objectMapper.writeValueAsString(PlayerEvent("SESSION_REVOKED", null, null))))
			} }.isSuccess
			if (sent) metrics.delivered("SESSION_REVOKED") else metrics.deliveryFailed("SESSION_REVOKED")
			closeAndUnregister(connection.session, CloseStatus.POLICY_VIOLATION.withReason("session.revoked"))
		}
	}

	@PreDestroy
	fun shutdown() {
		cleanup.shutdownNow()
		(connections.keys + authenticationDeadlines.keys).forEach { closeAndUnregister(it, CloseStatus.GOING_AWAY) }
	}

	internal fun evictExpired(now: Instant = Instant.now()) {
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

	private companion object {
		val AUTHENTICATION_TIMEOUT: Duration = Duration.ofSeconds(10)
		val HEARTBEAT_TIMEOUT: Duration = Duration.ofSeconds(35)
	}
}
