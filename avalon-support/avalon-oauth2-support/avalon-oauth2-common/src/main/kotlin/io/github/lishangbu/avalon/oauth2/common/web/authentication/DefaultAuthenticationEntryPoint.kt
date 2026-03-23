package io.github.lishangbu.avalon.oauth2.common.web.authentication

import io.github.lishangbu.avalon.oauth2.common.result.SecurityErrorResultCode
import io.github.lishangbu.avalon.web.util.JsonResponseWriter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import tools.jackson.databind.json.JsonMapper

/**
 * 统一的认证入口点 在未认证或认证失败时返回统一的 JSON 错误响应（HTTP 401 + 业务错误码） 被 [ExceptionTranslationFilter] 在需要发起认证流程时调用
 *
 * @author lishangbu
 * @since 2025/8/22
 */
class DefaultAuthenticationEntryPoint(
    jsonMapper: JsonMapper,
) : AuthenticationEntryPoint {
    private val jsonMapper = jsonMapper

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        log.error(
            "AuthenticationEntryPoint invoked for request [{}], reason=[{}]",
            request.requestURI,
            authException.message,
        )

        JsonResponseWriter.writeFailedResponse(
            response,
            jsonMapper,
            HttpStatus.UNAUTHORIZED,
            SecurityErrorResultCode.UNAUTHORIZED,
            authException.message.orEmpty(),
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(DefaultAuthenticationEntryPoint::class.java)
    }
}
