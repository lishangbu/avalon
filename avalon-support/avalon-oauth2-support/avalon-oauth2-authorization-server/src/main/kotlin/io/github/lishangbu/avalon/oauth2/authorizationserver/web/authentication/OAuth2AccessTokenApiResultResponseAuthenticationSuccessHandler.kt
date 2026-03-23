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
 * OAuth2 访问令牌成功响应处理器
 *
 * 在认证成功后写回统一的令牌响应，并记录认证日志
 */
class OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler
    @JvmOverloads
    constructor(
        authenticationLogRecorder: AuthenticationLogRecorder? = AuthenticationLogRecorder.noop(),
        /** 授权服务 */
        private val authorizationService: OAuth2AuthorizationService? = null,
        /** OAuth2 属性 */
        private val oauth2Properties: Oauth2Properties? = null,
        jsonMapper: JsonMapper,
    ) : AuthenticationSuccessHandler {
        /** 日志记录器 */
        private val logger: Log = LogFactory.getLog(javaClass)

        /** 访问令牌响应定制器 */
        private var accessTokenResponseCustomizer: Consumer<OAuth2AccessTokenAuthenticationContext>? =
            null

        /** 认证日志记录器 */
        private val authenticationLogRecorder: AuthenticationLogRecorder =
            authenticationLogRecorder ?: AuthenticationLogRecorder.noop()

        /** JSON 映射器 */
        private val jsonMapper: JsonMapper =
            requireNotNull(jsonMapper) { "jsonMapper cannot be null" }

        /** 将访问令牌响应写入统一 JSON，并记录认证成功日志 */
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

        /** 记录本次令牌签发对应的认证日志 */
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

        /** 按请求参数、授权记录和主体信息的优先级解析用户名 */
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

        /** 从已保存授权记录中回查主体名称 */
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

        /** 从认证结果或客户端主体中解析客户端 ID */
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

        /** 优先从请求参数解析授权类型，缺失时回退到附加参数 */
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

        /** 解析用户名请求参数名 */
        private fun resolveUsernameParameterName(): String {
            if (oauth2Properties == null) {
                return "username"
            }
            val configured = oauth2Properties.usernameParameterName
            return normalize(configured) ?: "username"
        }

        /** 按代理头优先级解析客户端 IP */
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

        /** 裁剪并过滤空白字符串 */
        private fun normalize(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }

        /** 构建统一返回的令牌响应体 */
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
