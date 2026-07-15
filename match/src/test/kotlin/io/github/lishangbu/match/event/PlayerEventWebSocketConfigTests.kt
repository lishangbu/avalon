package io.github.lishangbu.match.event

import io.github.lishangbu.common.web.CorsProperties
import io.github.lishangbu.match.trainer.TrainerSessionRegistry
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import tools.jackson.databind.json.JsonMapper
import java.time.Clock

class PlayerEventWebSocketConfigTests {
	@Test
	fun `websocket handshake uses the exact shared HTTP origins`() {
		val registry = Mockito.mock(WebSocketHandlerRegistry::class.java)
		val registration = Mockito.mock(WebSocketHandlerRegistration::class.java)
		Mockito.`when`(registry.addHandler(Mockito.any(), Mockito.eq("/api/player/events")))
			.thenReturn(registration)
		Mockito.`when`(registration.setAllowedOrigins(Mockito.anyString(), Mockito.anyString()))
			.thenReturn(registration)
		val origins = listOf("https://admin.example", "https://fallback.example")
		val config = PlayerEventWebSocketConfig(
			TrainerSessionRegistry(),
			Mockito.mock(PlayerEventHub::class.java),
			JsonMapper.builder().build(),
			CorsProperties(origins),
			Mockito.mock(PlayerEventConnectionRateLimiter::class.java),
			Clock.systemUTC(),
		)

		config.registerWebSocketHandlers(registry)

		Mockito.verify(registration).setAllowedOrigins(*origins.toTypedArray())
	}
}
