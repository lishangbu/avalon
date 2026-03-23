package io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication

import io.github.lishangbu.avalon.oauth2.common.result.SecurityErrorResultCode
import io.github.lishangbu.avalon.web.util.JsonResponseWriter
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import tools.jackson.databind.json.JsonMapper
import java.io.IOException

/**
 * 授权端点失败响应处理器
 *
 * 将授权端点认证异常转换为统一的错误响应
 */
class AuthorizationEndpointErrorResponseHandler(
    jsonMapper: JsonMapper,
) : AuthenticationFailureHandler {
    /** JSON 映射器 */
    private val jsonMapper = jsonMapper

    /** 处理认证失败 */
    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        var errorMessage = exception.message
        if (errorMessage == null && exception is OAuth2AuthenticationException) {
            val error: OAuth2Error? = exception.error
            if (error != null) {
                errorMessage = error.description
                if (errorMessage == null) {
                    errorMessage = error.errorCode
                }
            }
        }
        JsonResponseWriter.writeFailedResponse(
            response,
            jsonMapper,
            HttpStatus.UNAUTHORIZED,
            SecurityErrorResultCode.UNAUTHORIZED,
            errorMessage.orEmpty(),
        )
    }
}
