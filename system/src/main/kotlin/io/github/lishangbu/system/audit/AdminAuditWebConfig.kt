package io.github.lishangbu.system.audit

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration(proxyBeanMethods = false)
class AdminAuditWebConfig(private val audit: AdminAuditService) : WebMvcConfigurer {
	override fun addInterceptors(registry: InterceptorRegistry) {
		registry.addInterceptor(AdminAuditInterceptor(audit)).addPathPatterns("/api/system/**")
	}
}
