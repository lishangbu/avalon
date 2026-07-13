package io.github.lishangbu.match.event

import io.github.lishangbu.match.trainer.TrainerSelection
import io.github.lishangbu.match.trainer.TrainerSessionRegistry
import io.github.lishangbu.security.oauth.BearerTokenAuthenticationManagerResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketSession
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class PlayerEventWebSocketHandlerTests {
	private val now = Instant.parse("2026-07-13T00:00:00Z")
	private val clock = Clock.fixed(now, ZoneOffset.UTC)
	private val objectMapper = JsonMapper.builder().build()

	@Test
	fun `first frame authenticates oauth and trainer session then heartbeat keeps presence alive`() {
		val tokens = Mockito.mock(BearerTokenAuthenticationManagerResolver::class.java)
		val principal = DefaultOAuth2AuthenticatedPrincipal(mapOf("account_id" to "7"), emptyList())
		Mockito.`when`(tokens.authenticate("access-token"))
			.thenReturn(UsernamePasswordAuthenticationToken.authenticated(principal, null, emptyList()))
		val registry = TrainerSessionRegistry(credentialGenerator = { "trainer-token" })
		registry.enter(TrainerSelection(7, 11), now)
		val socket = socket()
		val hub = PlayerEventHub(objectMapper, registry)
		val handler = PlayerEventWebSocketHandler(tokens, registry, hub, objectMapper, clock)

		handler.handleMessage(socket, TextMessage("""{"type":"AUTHENTICATE","accessToken":"access-token","trainerCredential":"trainer-token"}"""))
		handler.handleMessage(socket, TextMessage("""{"type":"HEARTBEAT"}"""))

		assertThat(sentMessages(socket).map(TextMessage::getPayload))
			.anyMatch { it.contains("AUTHENTICATED") }
			.anyMatch { it.contains("HEARTBEAT_ACK") }
		assertThat(registry.isOnline(11, now.plusSeconds(44))).isTrue()
		hub.shutdown()
	}

	@Test
	fun `anonymous connection is closed after first frame deadline`() {
		val registry = TrainerSessionRegistry()
		val socket = socket()
		val hub = PlayerEventHub(objectMapper, registry)
		hub.connected(socket, now)

		hub.evictExpired(now.plusSeconds(11))

		Mockito.verify(socket).close(Mockito.any(CloseStatus::class.java))
		hub.shutdown()
	}

	@Test
	fun `failed delivery unregisters connection instead of retrying it forever`() {
		val registry = TrainerSessionRegistry(credentialGenerator = { "trainer-token" })
		val current = Instant.now()
		registry.enter(TrainerSelection(7, 11), current)
		val socket = socket()
		Mockito.doThrow(IllegalStateException("broken socket")).`when`(socket)
			.sendMessage(Mockito.any())
		val hub = PlayerEventHub(objectMapper, registry)
		hub.register(7, 11, "trainer-token", socket, current)

		hub.publish(listOf(11), PlayerEvent("MATCH_CHANGED", "21", 1))
		hub.publish(listOf(11), PlayerEvent("MATCH_CHANGED", "21", 2))

		Mockito.verify(socket, Mockito.times(1)).sendMessage(Mockito.any())
		hub.shutdown()
	}

	@Test
	fun `heartbeat timeout closes an authenticated connection`() {
		val registry = TrainerSessionRegistry(credentialGenerator = { "trainer-token" })
		registry.enter(TrainerSelection(7, 11), now)
		val socket = socket()
		val hub = PlayerEventHub(objectMapper, registry)
		hub.register(7, 11, "trainer-token", socket, now)

		hub.evictExpired(now.plusSeconds(36))

		Mockito.verify(socket).close(Mockito.argThat<CloseStatus> { it.reason == "heartbeat.timeout" })
		hub.shutdown()
	}

	@Test
	fun `account revocation notifies then closes every account connection`() {
		val registry = TrainerSessionRegistry(credentialGenerator = { "trainer-token" })
		registry.enter(TrainerSelection(7, 11), now)
		val socket = socket()
		val hub = PlayerEventHub(objectMapper, registry)
		hub.register(7, 11, "trainer-token", socket, now)

		hub.revokeAccount(7)

		assertThat(sentMessages(socket).single().payload).contains("SESSION_REVOKED")
		Mockito.verify(socket).close(Mockito.argThat<CloseStatus> { it.reason == "session.revoked" })
		hub.shutdown()
	}

	private fun socket(): WebSocketSession = Mockito.mock(WebSocketSession::class.java).also { socket ->
		Mockito.`when`(socket.attributes).thenReturn(mutableMapOf())
		Mockito.`when`(socket.isOpen).thenReturn(true)
	}

	private fun sentMessages(socket: WebSocketSession): List<TextMessage> {
		val captor = org.mockito.ArgumentCaptor.forClass(org.springframework.web.socket.WebSocketMessage::class.java)
		Mockito.verify(socket, Mockito.atLeastOnce()).sendMessage(captor.capture())
		return captor.allValues.filterIsInstance<TextMessage>()
	}
}
