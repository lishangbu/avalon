package io.github.lishangbu.match.event

import io.github.lishangbu.match.trainer.TrainerSessionRegistry
import io.github.lishangbu.security.oauth.BearerTokenAuthenticationManagerResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@Configuration(proxyBeanMethods = false)
@EnableWebSocket
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class PlayerEventWebSocketConfig(
	tokens: BearerTokenAuthenticationManagerResolver,
	sessions: TrainerSessionRegistry,
	hub: PlayerEventHub,
	objectMapper: ObjectMapper,
) : WebSocketConfigurer {
	private val handler = PlayerEventWebSocketHandler(tokens, sessions, hub, objectMapper, Clock.systemUTC())
	override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
		registry.addHandler(handler, "/api/player/events")
	}
}
