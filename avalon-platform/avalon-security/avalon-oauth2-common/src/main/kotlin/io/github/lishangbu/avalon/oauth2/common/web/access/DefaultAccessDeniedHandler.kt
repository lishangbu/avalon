package io.github.lishangbu.avalon.oauth2.common.web.access

import io.github.lishangbu.avalon.oauth2.common.result.SecurityErrorResultCode
import io.github.lishangbu.avalon.web.util.JsonResponseWriter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import tools.jackson.databind.json.JsonMapper

/**
 * 访问拒绝响应处理器
 *
 * 将已认证但无权限的请求转换为统一的 JSON 错误响应
 */
class DefaultAccessDeniedHandler(
    jsonMapper: JsonMapper,
) : AccessDeniedHandler {
    /** JSON 映射器 */
    private val jsonMapper = jsonMapper

    /** 将访问拒绝异常写入 403 响应 */
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        log.error(
            "AccessDeniedHandler invoked for request [{}], reason [{}]",
            request.requestURI,
            accessDeniedException.message,
        )

        JsonResponseWriter.writeFailedResponse(
            response,
            jsonMapper,
            HttpStatus.FORBIDDEN,
            SecurityErrorResultCode.FORBIDDEN,
            accessDeniedException.message.orEmpty(),
        )
    }

    companion object {
        /** 日志记录器 */
        private val log = LoggerFactory.getLogger(DefaultAccessDeniedHandler::class.java)
    }
}
