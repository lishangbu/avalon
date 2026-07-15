package io.github.lishangbu.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class RequestCorrelationFilter : OncePerRequestFilter() {
	override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
		val requestId = request.getHeader(REQUEST_ID_HEADER)
			?.trim()
			?.takeIf { it.length in 1..128 }
			?: UUID.randomUUID().toString()
		response.setHeader(REQUEST_ID_HEADER, requestId)
		MDC.putCloseable("requestId", requestId).use {
			filterChain.doFilter(request, response)
		}
	}

	private companion object {
		const val REQUEST_ID_HEADER = "X-Request-Id"
	}
}
