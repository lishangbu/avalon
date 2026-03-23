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
 * 未认证请求入口处理器
 *
 * 将未认证或认证失败请求转换为统一的 JSON 错误响应
 */
class DefaultAuthenticationEntryPoint(
    jsonMapper: JsonMapper,
) : AuthenticationEntryPoint {
    /** JSON 映射器 */
    private val jsonMapper = jsonMapper

    /** 将认证异常写入 401 响应 */
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
        /** 日志记录器 */
        private val log = LoggerFactory.getLogger(DefaultAuthenticationEntryPoint::class.java)
    }
}
