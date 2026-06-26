package io.github.lishangbu.security.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 安全模块运行时配置。
 *
 * OAuth client、token 策略和密钥等持久化资源由数据库管理；配置文件只保留启停和
 * issuer 这类应用装配参数。
 */
@ConfigurationProperties("backend.security")
data class SecurityProperties(
	val enabled: Boolean = true,
	val issuer: String = "http://localhost:8080",
)
