package io.github.lishangbu.system.audit

import cn.dev33.satoken.stp.StpUtil
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.HandlerInterceptor
import java.util.UUID

internal class AdminAuditInterceptor(private val audit: AdminAuditService) : HandlerInterceptor {
	override fun afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Any, ex: Exception?) {
		if (request.method !in MUTATING_METHODS || !StpUtil.isLogin()) return
		val command = AdminAuditCommand(
			actorAccountId = StpUtil.getLoginIdAsLong(),
			httpMethod = request.method,
			requestPath = request.requestURI,
			outcome = if (response.status < 400) "SUCCESS" else "FAILURE",
			responseStatus = response.status,
			requestId = request.getHeader("X-Request-Id")?.takeIf(String::isNotBlank) ?: UUID.randomUUID().toString(),
			remoteAddress = request.remoteAddr ?: "unknown",
		)
		runCatching { audit.append(command) }
			.onFailure { logger.error("Failed to append admin audit record", it) }
	}

	private companion object {
		val logger = LoggerFactory.getLogger(AdminAuditInterceptor::class.java)
		val MUTATING_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
	}
}
