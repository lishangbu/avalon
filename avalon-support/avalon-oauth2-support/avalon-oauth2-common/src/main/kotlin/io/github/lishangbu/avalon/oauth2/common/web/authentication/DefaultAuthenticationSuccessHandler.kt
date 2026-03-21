package io.github.lishangbu.avalon.oauth2.common.web.authentication

import io.github.lishangbu.avalon.web.util.JsonResponseWriter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import tools.jackson.databind.json.JsonMapper
import java.util.*

/**
 * 默认认证成功处理器 在认证成功时返回一个统一的 JSON 成功响应
 *
 * @author lishangbu
 * @since 2025/8/23
 */
class DefaultAuthenticationSuccessHandler(
    jsonMapper: JsonMapper,
) : AuthenticationSuccessHandler {
    private val jsonMapper: JsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper")

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        JsonResponseWriter.writeSuccessResponse(response, jsonMapper)
    }
}
