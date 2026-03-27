package io.github.lishangbu.avalon.oauth2.common.web.authentication

import io.github.lishangbu.avalon.web.util.JsonResponseWriter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import tools.jackson.databind.json.JsonMapper

/**
 * 认证成功响应处理器
 *
 * 在认证成功后写回统一的 JSON 成功结果
 */
class DefaultAuthenticationSuccessHandler(
    jsonMapper: JsonMapper,
) : AuthenticationSuccessHandler {
    /** JSON 映射器 */
    private val jsonMapper = jsonMapper

    /** 将认证成功结果写入响应体 */
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        JsonResponseWriter.writeSuccessResponse(response, jsonMapper)
    }
}
