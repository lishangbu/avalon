package io.github.lishangbu.security.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * 本地或测试环境显式关闭安全时使用的兜底配置。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "false")
class SecurityDisabledConfig {
	/**
	 * 关闭所有鉴权和 CSRF 约束，避免业务测试被安全链阻断。
	 */
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	fun disabledSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
		http
			.authorizeHttpRequests { authorize ->
				authorize.anyRequest().permitAll()
			}
			.csrf { csrf ->
				csrf.disable()
			}
		return http.build()
	}
}
