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
 * An implementation of an `AuthenticationFailureHandler` used for handling an
 * `OAuth2AuthenticationException` and returning the `OAuth2Error` OAuth 2.0 Error Response
 *
 * @see AuthenticationFailureHandler
 * @see OAuth2ErrorHttpMessageConverter
 * @author Dmitriy Dubson
 * @author lishangbu
 * @since 2025/8/25
 */
class OAuth2ErrorApiResultAuthenticationFailureHandler
    @JvmOverloads
    constructor(
        authenticationLogRecorder: AuthenticationLogRecorder? = AuthenticationLogRecorder.noop(),
        private val oauth2Properties: Oauth2Properties? = null,
        jsonMapper: JsonMapper,
    ) : AuthenticationFailureHandler {
        private val logger: Log = LogFactory.getLog(javaClass)
        private val authenticationLogRecorder: AuthenticationLogRecorder =
            authenticationLogRecorder ?: AuthenticationLogRecorder.noop()
        private val jsonMapper = jsonMapper

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

        private fun resolveUsernameParameterName(): String {
            if (oauth2Properties == null) {
                return "username"
            }
            val configured = oauth2Properties.usernameParameterName
            return normalize(configured) ?: "username"
        }

        private fun resolveClientId(request: HttpServletRequest): String? {
            val clientId = normalize(request.getParameter("client_id"))
            if (clientId != null) {
                return clientId
            }
            val authorization = request.getHeader("Authorization")
            if (authorization == null || !authorization.startsWith("Basic ")) {
                return null
            }
            val base64Credentials = authorization.substring("Basic ".length).trim { it <= ' ' }
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

        private fun resolveClientIp(request: HttpServletRequest): String? {
            val forwardedFor = request.getHeader("X-Forwarded-For")
            if (forwardedFor != null && forwardedFor.isNotBlank()) {
                val parts = forwardedFor.split(",", limit = 2)
                return normalize(parts[0].trim { it <= ' ' })
            }
            val realIp = request.getHeader("X-Real-IP")
            if (realIp != null && realIp.isNotBlank()) {
                return normalize(realIp.trim { it <= ' ' })
            }
            return normalize(request.remoteAddr)
        }

        private fun normalize(value: String?): String? {
            if (value == null) {
                return null
            }
            val trimmed = value.trim { it <= ' ' }
            return if (trimmed.isEmpty()) null else trimmed
        }

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
                return trimmed ?: null
            }
            val colonPrefix = OAuth2ErrorCodes.INVALID_GRANT + ":"
            if (lower.startsWith(colonPrefix)) {
                val trimmed = normalize(normalized.substring(colonPrefix.length))
                return trimmed ?: null
            }
            if (lower.startsWith(OAuth2ErrorCodes.INVALID_GRANT)) {
                val trimmed = normalize(normalized.substring(OAuth2ErrorCodes.INVALID_GRANT.length))
                return trimmed ?: null
            }
            return normalized
        }

        private data class ResolvedError(
            val code: String?,
            val description: String?,
        )
    }
