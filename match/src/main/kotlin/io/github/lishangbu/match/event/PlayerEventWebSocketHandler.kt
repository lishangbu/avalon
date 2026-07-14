package io.github.lishangbu.match.event

import io.github.lishangbu.match.trainer.TrainerSessionRegistry
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.Instant

/** 首帧完成 Sa-Token 与 Trainer Session 双认证；后续帧只接受 Presence 心跳。 */
class PlayerEventWebSocketHandler(
	private val tokenAccount: (String) -> Long?,
	private val trainerSessions: TrainerSessionRegistry,
	private val events: PlayerEventHub,
	private val objectMapper: ObjectMapper,
	private val clock: Clock,
) : TextWebSocketHandler() {
	override fun afterConnectionEstablished(session: WebSocketSession) {
		events.connected(session, Instant.now(clock))
	}

	override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
		val payload = runCatching { objectMapper.readTree(message.payload) }.getOrNull()
			?: return session.close(CloseStatus.BAD_DATA)
		val trainerId = session.attributes[TRAINER_ID] as? Long
		if (trainerId == null && payload.path("type").asString() != "AUTHENTICATE") session.close(CloseStatus.BAD_DATA)
		else if (trainerId == null) authenticate(
			session,
			payload.path("accessToken").asString(),
			payload.path("trainerCredential").asString(),
			payload.path("reconnect").asBoolean(false),
		)
		else heartbeat(session, payload.path("type").asString(), trainerId)
	}

	override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
		events.unregister(session, status.reason?.takeIf(String::isNotBlank) ?: "client")
	}

	private fun authenticate(session: WebSocketSession, accessToken: String, credential: String, reconnect: Boolean) {
		val accountId = tokenAccount(accessToken)
		val trainerSession = accountId?.let { trainerSessions.authenticate(it, credential, Instant.now(clock)) }
		if (trainerSession == null) {
			events.authenticationFailed()
			return session.close(CloseStatus.NOT_ACCEPTABLE.withReason("authentication.required"))
		}
		session.attributes[TRAINER_ID] = trainerSession.trainerId
		session.attributes[ACCOUNT_ID] = trainerSession.accountId
		session.attributes[CREDENTIAL] = credential
		events.register(trainerSession.accountId, trainerSession.trainerId, credential, session, Instant.now(clock), reconnect)
		session.sendMessage(TextMessage(objectMapper.writeValueAsString(PlayerEvent("AUTHENTICATED", trainerSession.trainerId.toString(), null))))
	}

	private fun heartbeat(session: WebSocketSession, type: String, trainerId: Long) {
		val accountId = session.attributes[ACCOUNT_ID] as Long
		val credential = session.attributes[CREDENTIAL] as String
		if (type != "HEARTBEAT" || !events.heartbeat(accountId, credential, session, Instant.now(clock))) {
			return session.close(CloseStatus.NOT_ACCEPTABLE.withReason("trainer-session.invalid"))
		}
		session.sendMessage(TextMessage(objectMapper.writeValueAsString(PlayerEvent("HEARTBEAT_ACK", trainerId.toString(), null))))
	}

	private companion object {
		const val TRAINER_ID = "player.trainerId"
		const val ACCOUNT_ID = "player.accountId"
		const val CREDENTIAL = "player.trainerCredential"
	}
}
