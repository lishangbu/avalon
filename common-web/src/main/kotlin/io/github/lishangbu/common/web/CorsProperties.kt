package io.github.lishangbu.common.web

import org.springframework.boot.context.properties.ConfigurationProperties

/** Exact browser origins allowed to access both HTTP APIs and WebSocket handshakes. */
@ConfigurationProperties("backend.cors")
data class CorsProperties(
	val allowedOrigins: List<String> = listOf(
		"http://localhost:5173",
		"http://127.0.0.1:5173",
		"http://localhost:5174",
		"http://127.0.0.1:5174",
	),
)
