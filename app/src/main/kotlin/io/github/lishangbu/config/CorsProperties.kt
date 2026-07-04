package io.github.lishangbu.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 管理端访问后端 API 的跨域来源配置。
 */
@ConfigurationProperties("backend.cors")
data class CorsProperties(
	val allowedOrigins: List<String> = listOf(
		"http://localhost:5173",
		"http://127.0.0.1:5173",
		"http://localhost:5174",
		"http://127.0.0.1:5174",
	),
)
