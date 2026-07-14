package io.github.lishangbu.security.config

import cn.dev33.satoken.stp.StpUtil
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/** 只在受保护的 API 与指标端点上读取 Sa-Token 登录上下文。 */
@Component
class SaTokenAuthenticationInterceptor : HandlerInterceptor {
	override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
		if (request.method == "OPTIONS") return true
		val path = request.requestURI
		if (path.startsWith("/api/") && path !in publicApiPaths) {
			StpUtil.checkLogin()
		}
		if (path == "/actuator/metrics" || path.startsWith("/actuator/metrics/")) {
			StpUtil.checkLogin()
			StpUtil.checkPermission("security:admin")
		}
		return true
	}

	private companion object {
		val publicApiPaths = setOf("/api/auth/login", "/api/player/events")
	}
}
