package io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication

import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecord
import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecorder
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import io.github.lishangbu.avalon.web.result.DefaultErrorResultCode
import io.github.lishangbu.avalon.web.util.JsonResponseWriter
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import tools.jackson.databind.json.JsonMapper
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*

/**
 * OAuth2 认证失败处理器
 *
 * 将认证异常转换为统一的 JSON 错误响应并记录认证日志
 */
class OAuth2ErrorApiResultAuthenticationFailureHandler
    @JvmOverloads
    constructor(
        authenticationLogRecorder: AuthenticationLogRecorder? = AuthenticationLogRecorder.noop(),
        /** OAuth2 属性 */
        private val oauth2Properties: Oauth2Properties? = null,
        jsonMapper: JsonMapper,
    ) : AuthenticationFailureHandler {
        /** 日志记录器 */
        private val logger: Log = LogFactory.getLog(javaClass)

        /** 认证日志记录器 */
        private val authenticationLogRecorder: AuthenticationLogRecorder =
            authenticationLogRecorder ?: AuthenticationLogRecorder.noop()

        /** JSON 映射器 */
        private val jsonMapper = jsonMapper

        /** 处理认证失败 */
        @Throws(IOException::class, ServletException::class)
        override fun onAuthenticationFailure(
            request: HttpServletRequest,
            response: HttpServletResponse,
            authenticationException: AuthenticationException,
        ) {
            val resolvedError = resolveError(authenticationException)
            recordAuthenticationFailure(request, resolvedError)
            writeFailedResponse(
                response,
                normalize(resolvedError.code),
                normalize(resolvedError.description),
            )
            if (authenticationException !is OAuth2AuthenticationException && logger.isWarnEnabled) {
                logger.warn(
                    AuthenticationException::class.java.simpleName +
                        " must be of type " +
                        OAuth2AuthenticationException::class.java.name +
                        " but was " +
                        authenticationException.javaClass.name,
                )
            }
        }

        /** 解析错误 */
        private fun resolveError(authenticationException: AuthenticationException?): ResolvedError {
            if (authenticationException is OAuth2AuthenticationException) {
                val error = authenticationException.error
                val errorCode = error?.errorCode
                var errorDescription = error?.description
                if (normalize(errorDescription) == null) {
                    val message = normalize(authenticationException.message)
                    if (message != null && !message.equals(normalize(errorCode), ignoreCase = true)) {
                        errorDescription = message
                    }
                }
                return ResolvedError(errorCode, sanitizeDescription(errorDescription))
            }
            if (authenticationException == null) {
                return ResolvedError(null, null)
            }
            return ResolvedError(
                authenticationException.javaClass.simpleName,
                sanitizeDescription(authenticationException.message),
            )
        }

        /** 记录认证失败 */
        private fun recordAuthenticationFailure(
            request: HttpServletRequest,
            error: ResolvedError,
        ) {
            try {
                var errorMessage = sanitizeDescription(error.description)
                if (errorMessage == null) {
                    val code = normalize(error.code)
                    if (code != null && OAuth2ErrorCodes.INVALID_GRANT != code) {
                        errorMessage = code
                    }
                }
                val record =
                    AuthenticationLogRecord(
                        normalize(request.getParameter(resolveUsernameParameterName())),
                        resolveClientId(request),
                        normalize(request.getParameter("grant_type")),
                        resolveClientIp(request),
                        normalize(request.getHeader("User-Agent")),
                        false,
                        errorMessage,
                        Instant.now(),
                    )
                authenticationLogRecorder.record(record)
            } catch (ex: Exception) {
                if (logger.isWarnEnabled) {
                    logger.warn("Failed to record authentication log", ex)
                }
            }
        }

        /** 解析用户名参数名 */
        private fun resolveUsernameParameterName(): String {
            if (oauth2Properties == null) {
                return "username"
            }
            val configured = oauth2Properties.usernameParameterName
            return normalize(configured) ?: "username"
        }

        /** 解析客户端 ID */
        private fun resolveClientId(request: HttpServletRequest): String? {
            val clientId = normalize(request.getParameter("client_id"))
            if (clientId != null) {
                return clientId
            }
            val authorization = request.getHeader("Authorization")
            if (authorization == null || !authorization.startsWith("Basic ")) {
                return null
            }
            val base64Credentials = authorization.substring("Basic ".length).trim()
            if (base64Credentials.isEmpty()) {
                return null
            }
            try {
                val decoded = Base64.getDecoder().decode(base64Credentials)
                val credentials = String(decoded, StandardCharsets.UTF_8)
                val delimiterIndex = credentials.indexOf(':')
                if (delimiterIndex > 0) {
                    return normalize(credentials.substring(0, delimiterIndex))
                }
            } catch (ex: IllegalArgumentException) {
                if (logger.isTraceEnabled) {
                    logger.trace("Failed to decode client credentials", ex)
                }
            }
            return null
        }

        /** 解析客户端 IP */
        private fun resolveClientIp(request: HttpServletRequest): String? {
            val forwardedFor = request.getHeader("X-Forwarded-For")
            if (forwardedFor != null && forwardedFor.isNotBlank()) {
                return normalize(forwardedFor.substringBefore(','))
            }
            val realIp = request.getHeader("X-Real-IP")
            if (realIp != null && realIp.isNotBlank()) {
                return normalize(realIp)
            }
            return normalize(request.remoteAddr)
        }

        /** 清理字符串值 */
        private fun normalize(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }

        /** 写入失败响应 */
        private fun writeFailedResponse(
            response: HttpServletResponse,
            errorCode: String?,
            errorDescription: String?,
        ) {
            val code = normalize(errorCode)
            val description = sanitizeDescription(errorDescription)
            if (description != null) {
                JsonResponseWriter.writeFailedResponse(
                    response,
                    jsonMapper,
                    HttpStatus.BAD_REQUEST,
                    DefaultErrorResultCode.BAD_REQUEST,
                    description,
                )
                return
            }
            if (code == null || OAuth2ErrorCodes.INVALID_GRANT == code) {
                JsonResponseWriter.writeFailedResponse(
                    response,
                    jsonMapper,
                    HttpStatus.BAD_REQUEST,
                    DefaultErrorResultCode.BAD_REQUEST,
                )
                return
            }
            JsonResponseWriter.writeFailedResponse(
                response,
                jsonMapper,
                HttpStatus.BAD_REQUEST,
                DefaultErrorResultCode.BAD_REQUEST,
                code,
            )
        }

        /** 清理错误描述 */
        private fun sanitizeDescription(description: String?): String? {
            val normalized = normalize(description)
            if (normalized == null) {
                return null
            }
            val lower = normalized.lowercase(Locale.ROOT)
            if (OAuth2ErrorCodes.INVALID_GRANT == lower) {
                return null
            }
            val prefix = "[" + OAuth2ErrorCodes.INVALID_GRANT + "]"
            if (lower.startsWith(prefix)) {
                val trimmed = normalize(normalized.substring(prefix.length))
                return trimmed
            }
            val colonPrefix = OAuth2ErrorCodes.INVALID_GRANT + ":"
            if (lower.startsWith(colonPrefix)) {
                val trimmed = normalize(normalized.substring(colonPrefix.length))
                return trimmed
            }
            if (lower.startsWith(OAuth2ErrorCodes.INVALID_GRANT)) {
                val trimmed = normalize(normalized.substring(OAuth2ErrorCodes.INVALID_GRANT.length))
                return trimmed
            }
            return normalized
        }

        private data class ResolvedError(
            /** 状态码 */
            val code: String?,
            /** 描述 */
            val description: String?,
        )
    }
