package io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication

import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecord
import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecorder
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import io.github.lishangbu.avalon.web.util.JsonResponseWriter
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationContext
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import tools.jackson.databind.json.JsonMapper
import java.io.IOException
import java.security.Principal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.function.Consumer

/**
 * An implementation of an `AuthenticationSuccessHandler` used for handling an
 * `OAuth2AccessTokenAuthenticationToken` and returning the `OAuth2AccessTokenResponse` Access
 */
// Token Response

/**
 * @see AuthenticationSuccessHandler
 * @see OAuth2AccessTokenResponseHttpMessageConverter
 * @author Dmitriy Dubson
 * @author lishangbu
 * @since 2025/8/25
 */
class OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler
    @JvmOverloads
    constructor(
        authenticationLogRecorder: AuthenticationLogRecorder? = AuthenticationLogRecorder.noop(),
        private val authorizationService: OAuth2AuthorizationService? = null,
        private val oauth2Properties: Oauth2Properties? = null,
        jsonMapper: JsonMapper,
    ) : AuthenticationSuccessHandler {
        private val logger: Log = LogFactory.getLog(javaClass)
        private var accessTokenResponseCustomizer: Consumer<OAuth2AccessTokenAuthenticationContext>? =
            null
        private val authenticationLogRecorder: AuthenticationLogRecorder =
            authenticationLogRecorder ?: AuthenticationLogRecorder.noop()
        private val jsonMapper: JsonMapper =
            requireNotNull(jsonMapper) { "jsonMapper cannot be null" }

        @Throws(IOException::class, ServletException::class)
        override fun onAuthenticationSuccess(
            request: HttpServletRequest,
            response: HttpServletResponse,
            authentication: Authentication,
        ) {
            if (authentication !is OAuth2AccessTokenAuthenticationToken) {
                if (logger.isErrorEnabled) {
                    logger.error(
                        Authentication::class.java.simpleName +
                            " must be of type " +
                            OAuth2AccessTokenAuthenticationToken::class.java.name +
                            " but was " +
                            authentication.javaClass.name,
                    )
                }
                val error =
                    OAuth2Error(
                        OAuth2ErrorCodes.SERVER_ERROR,
                        "Unable to process the access token response.",
                        null,
                    )
                throw OAuth2AuthenticationException(error)
            }

            val accessToken = authentication.accessToken
            val refreshToken: OAuth2RefreshToken? = authentication.refreshToken
            val additionalParameters = authentication.additionalParameters

            val builder =
                OAuth2AccessTokenResponse
                    .withToken(accessToken.tokenValue)
                    .tokenType(accessToken.tokenType)
                    .scopes(accessToken.scopes)
            if (accessToken.issuedAt != null && accessToken.expiresAt != null) {
                builder.expiresIn(
                    ChronoUnit.SECONDS.between(accessToken.issuedAt, accessToken.expiresAt),
                )
            }
            if (refreshToken != null) {
                builder.refreshToken(refreshToken.tokenValue)
            }
            if (additionalParameters.isNotEmpty()) {
                builder.additionalParameters(additionalParameters)
            }

            if (accessTokenResponseCustomizer != null) {
                // @formatter:off
                val accessTokenAuthenticationContext =
                    OAuth2AccessTokenAuthenticationContext
                        .with(authentication)
                        .accessTokenResponse(builder)
                        .build()
                // @formatter:on
                accessTokenResponseCustomizer?.accept(accessTokenAuthenticationContext)
                if (logger.isTraceEnabled) {
                    logger.trace("Customized access token response")
                }
            }

            val accessTokenResponse = builder.build()
            recordAuthenticationSuccess(request, authentication)
            JsonResponseWriter.writeSuccessResponse(
                response,
                jsonMapper,
                buildTokenResponseBody(accessTokenResponse),
            )
        }

        private fun recordAuthenticationSuccess(
            request: HttpServletRequest,
            accessTokenAuthentication: OAuth2AccessTokenAuthenticationToken,
        ) {
            try {
                val principal = accessTokenAuthentication.principal
                val grantType =
                    resolveGrantType(request, accessTokenAuthentication.additionalParameters)
                val username = resolveUsername(principal, request, accessTokenAuthentication, grantType)
                val clientId = resolveClientId(accessTokenAuthentication, principal)
                val record =
                    AuthenticationLogRecord(
                        normalize(username),
                        normalize(clientId),
                        normalize(grantType),
                        resolveClientIp(request),
                        normalize(request.getHeader("User-Agent")),
                        true,
                        null,
                        Instant.now(),
                    )
                authenticationLogRecorder.record(record)
            } catch (ex: Exception) {
                if (logger.isWarnEnabled) {
                    logger.warn("Failed to record authentication log", ex)
                }
            }
        }

        private fun resolveUsername(
            principal: Any?,
            request: HttpServletRequest,
            accessTokenAuthentication: OAuth2AccessTokenAuthenticationToken,
            grantType: String?,
        ): String? {
            if (AuthorizationGrantType.CLIENT_CREDENTIALS.value == grantType) {
                return null
            }
            val requestUsername = normalize(request.getParameter(resolveUsernameParameterName()))
            if (requestUsername != null) {
                return requestUsername
            }
            val authorizationUsername = resolveAuthorizationUsername(accessTokenAuthentication)
            if (authorizationUsername != null) {
                return authorizationUsername
            }
            if (principal == null) {
                return null
            }
            if (principal is OAuth2ClientAuthenticationToken) {
                return null
            }
            if (principal is Authentication) {
                return normalize(principal.name)
            }
            if (principal is Principal) {
                return normalize(principal.name)
            }
            return normalize(principal.toString())
        }

        private fun resolveAuthorizationUsername(
            accessTokenAuthentication: OAuth2AccessTokenAuthenticationToken?,
        ): String? {
            if (authorizationService == null || accessTokenAuthentication == null) {
                return null
            }
            val accessToken = accessTokenAuthentication.accessToken ?: return null
            val authorization =
                authorizationService.findByToken(accessToken.tokenValue, OAuth2TokenType.ACCESS_TOKEN)
                    ?: return null
            return normalize(authorization.principalName)
        }

        private fun resolveClientId(
            accessTokenAuthentication: OAuth2AccessTokenAuthenticationToken,
            principal: Any?,
        ): String? {
            if (accessTokenAuthentication.registeredClient != null) {
                return normalize(accessTokenAuthentication.registeredClient.clientId)
            }
            if (principal is OAuth2ClientAuthenticationToken) {
                return normalize(principal.name)
            }
            return null
        }

        private fun resolveGrantType(
            request: HttpServletRequest,
            additionalParameters: Map<String, Any>?,
        ): String? {
            val grantType = normalize(request.getParameter("grant_type"))
            if (grantType != null) {
                return grantType
            }
            if (additionalParameters != null) {
                val value = additionalParameters["grant_type"]
                if (value is String && value.isNotBlank()) {
                    return value
                }
            }
            return null
        }

        private fun resolveUsernameParameterName(): String {
            if (oauth2Properties == null) {
                return "username"
            }
            val configured = oauth2Properties.usernameParameterName
            return normalize(configured) ?: "username"
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

        private fun buildTokenResponseBody(
            accessTokenResponse: OAuth2AccessTokenResponse,
        ): Map<String, Any> {
            val body: MutableMap<String, Any> = LinkedHashMap()
            val accessToken = accessTokenResponse.accessToken
            body["access_token"] = accessToken.tokenValue
            body["token_type"] = accessToken.tokenType.value
            if (accessToken.issuedAt != null && accessToken.expiresAt != null) {
                body["expires_in"] =
                    ChronoUnit.SECONDS.between(accessToken.issuedAt, accessToken.expiresAt)
            }
            accessTokenResponse.refreshToken?.let { refreshToken ->
                body["refresh_token"] = refreshToken.tokenValue
            }
            val scopes = accessToken.scopes
            if (scopes != null && scopes.isNotEmpty()) {
                body["scope"] = scopes.joinToString(" ")
            }
            val additionalParameters = accessTokenResponse.additionalParameters
            if (additionalParameters != null && additionalParameters.isNotEmpty()) {
                body.putAll(additionalParameters)
            }
            return body
        }
    }
