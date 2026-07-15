package io.github.lishangbu.match.event

import cn.dev33.satoken.stp.StpUtil
import io.github.lishangbu.common.web.CorsProperties
import io.github.lishangbu.match.trainer.TrainerSessionRegistry
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties

@Configuration(proxyBeanMethods = false)
@EnableWebSocket
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CorsProperties::class)
class PlayerEventWebSocketConfig(
	sessions: TrainerSessionRegistry,
	hub: PlayerEventHub,
	objectMapper: ObjectMapper,
	private val corsProperties: CorsProperties,
	rateLimiter: PlayerEventConnectionRateLimiter,
	clock: Clock,
) : WebSocketConfigurer {
	private val handler = PlayerEventWebSocketHandler(
		tokenAccount = { token -> runCatching { StpUtil.getLoginIdByToken(token).toString().toLong() }.getOrNull() },
		trainerSessions = sessions,
		events = hub,
		objectMapper = objectMapper,
		clock = clock,
		authenticationAllowed = rateLimiter::tryAcquire,
	)
	override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
		registry.addHandler(handler, "/api/player/events")
			.setAllowedOrigins(*corsProperties.allowedOrigins.toTypedArray())
	}
}
