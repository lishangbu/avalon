package io.github.lishangbu.security.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/** 注册 Sa-Token 的登录校验和注解鉴权拦截器。 */
@Configuration(proxyBeanMethods = false)
class SaTokenWebConfig(
	private val apiPermissionInterceptor: ApiPermissionInterceptor,
	private val authenticationInterceptor: SaTokenAuthenticationInterceptor,
) : WebMvcConfigurer {
	override fun addInterceptors(registry: InterceptorRegistry) {
		registry.addInterceptor(authenticationInterceptor).addPathPatterns("/**")
		registry.addInterceptor(apiPermissionInterceptor).addPathPatterns("/**")
	}
}
