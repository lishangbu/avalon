package io.github.lishangbu.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * 只在应用装配边界为业务 API 开启跨域访问。
 */
@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class WebCorsConfig(
	private val corsProperties: CorsProperties,
) : WebMvcConfigurer {
	override fun addCorsMappings(registry: CorsRegistry) {
		registry.addMapping("/api/**")
			.allowedOrigins(*corsProperties.allowedOrigins.toTypedArray())
			.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
			.allowedHeaders("*")
			.allowCredentials(false)
			.maxAge(3600)
	}
}
