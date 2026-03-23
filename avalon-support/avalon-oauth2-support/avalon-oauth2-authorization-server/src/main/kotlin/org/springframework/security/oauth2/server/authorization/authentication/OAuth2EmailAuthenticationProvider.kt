package org.springframework.security.oauth2.server.authorization.authentication

import io.github.lishangbu.avalon.oauth2.authorizationserver.authentication.EmailAuthenticationToken
import io.github.lishangbu.avalon.oauth2.authorizationserver.login.LoginFailureTracker
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.core.log.LogMessage
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import java.security.Principal

/** 邮箱授权提供者日志记录器 */
private val EMAIL_PROVIDER_LOGGER: Logger = LogManager.getLogger()

/** 邮箱 ID 令牌类型 */
private val EMAIL_ID_TOKEN_TYPE = OAuth2TokenType(OidcParameterNames.ID_TOKEN)

/**
 * 邮箱授权模式认证提供者
 *
 * 处理 `email` 授权类型的客户端校验、邮箱验证码认证、令牌生成与授权保存
 */
class OAuth2EmailAuthenticationProvider
    @JvmOverloads
    constructor(
        /** 认证管理器 */
        private val authenticationManager: AuthenticationManager,
        authorizationService: OAuth2AuthorizationService?,
        tokenGenerator: OAuth2TokenGenerator<out OAuth2Token>?,
        /** 登录失败跟踪器 */
        private val loginFailureTracker: LoginFailureTracker? = null,
    ) : AuthenticationProvider {
        /** 授权服务 */
        private val authorizationService: OAuth2AuthorizationService =
            requireNotNull(authorizationService) { "authorizationService cannot be null" }

        /** 令牌生成器 */
        private val tokenGenerator: OAuth2TokenGenerator<out OAuth2Token> =
            requireNotNull(tokenGenerator) { "tokenGenerator cannot be null" }

        /** 校验邮箱授权请求并签发访问令牌 */
        override fun authenticate(authentication: Authentication): Authentication {
            val emailGrantAuthenticationToken =
                authentication as OAuth2EmailAuthorizationGrantAuthenticationToken

            val clientAuthenticationToken =
                emailGrantAuthenticationToken.principal as OAuth2ClientAuthenticationToken
            val registeredClient =
                requireNotNull(clientAuthenticationToken.registeredClient) {
                    "registeredClient cannot be null"
                }

            if (EMAIL_PROVIDER_LOGGER.isTraceEnabled) {
                EMAIL_PROVIDER_LOGGER.trace("Retrieved registered client")
            }

            if (
                !registeredClient.authorizationGrantTypes.contains(AuthorizationGrantTypeSupport.EMAIL)
            ) {
                if (EMAIL_PROVIDER_LOGGER.isDebugEnabled) {
                    EMAIL_PROVIDER_LOGGER.debug(
                        LogMessage.format(
                            "Invalid request: requested grant_type is not allowed for registered client '%s'",
                            registeredClient.id,
                        ),
                    )
                }
                throw OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT)
            }

            val email = emailGrantAuthenticationToken.email
            val emailCode = emailGrantAuthenticationToken.emailCode

            if (loginFailureTracker != null && loginFailureTracker.isEnabled()) {
                val remainingLock = loginFailureTracker.getRemainingLock(email)
                if (remainingLock != null) {
                    throw OAuth2AuthenticationException(
                        OAuth2Error(
                            OAuth2ErrorCodes.INVALID_GRANT,
                            buildLockMessage(remainingLock),
                            TOKEN_REQUEST_ERROR_URI,
                        ),
                    )
                }
            }

            val emailAuthenticationToken = EmailAuthenticationToken(email, emailCode)

            val emailAuthentication =
                try {
                    authenticationManager.authenticate(emailAuthenticationToken)
                } catch (ex: AuthenticationException) {
                    loginFailureTracker?.onFailure(email)
                    if (ex is OAuth2AuthenticationException) {
                        throw ex
                    }
                    throw OAuth2AuthenticationException(
                        OAuth2Error(
                            OAuth2ErrorCodes.INVALID_GRANT,
                            ex.message,
                            TOKEN_REQUEST_ERROR_URI,
                        ),
                        ex,
                    )
                }

            loginFailureTracker?.onSuccess(email)

            var authorizedScopes: Set<String> = registeredClient.scopes // Default to configured scopes
            val requestedScopes = emailGrantAuthenticationToken.scopes
            if (requestedScopes.isNotEmpty()) {
                val unauthorizedScopes = requestedScopes - registeredClient.scopes
                if (unauthorizedScopes.isNotEmpty()) {
                    throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_SCOPE)
                }

                authorizedScopes = LinkedHashSet(requestedScopes)
            }

            if (EMAIL_PROVIDER_LOGGER.isTraceEnabled) {
                EMAIL_PROVIDER_LOGGER.trace("Validated token request parameters")
            }

            val tokenContextBuilder =
                DefaultOAuth2TokenContext
                    .builder()
                    .registeredClient(registeredClient)
                    .principal(emailAuthentication)
                    .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                    .authorizedScopes(authorizedScopes)
                    .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                    .authorizationGrantType(AuthorizationGrantTypeSupport.EMAIL)
                    .authorizationGrant(emailGrantAuthenticationToken)

            // ----- Access token -----
            var tokenContext: OAuth2TokenContext =
                tokenContextBuilder.tokenType(OAuth2TokenType.ACCESS_TOKEN).build()

            val generatedAccessToken =
                tokenGenerator.generate(tokenContext)
                    ?: throw OAuth2AuthenticationException(
                        OAuth2Error(
                            OAuth2ErrorCodes.SERVER_ERROR,
                            "The token generator failed to generate the access token.",
                            TOKEN_REQUEST_ERROR_URI,
                        ),
                    )

            if (EMAIL_PROVIDER_LOGGER.isTraceEnabled) {
                EMAIL_PROVIDER_LOGGER.trace("Generated access token")
            }

            val authorizationBuilder =
                OAuth2Authorization
                    .withRegisteredClient(registeredClient)
                    .principalName(email)
                    .authorizationGrantType(AuthorizationGrantTypeSupport.EMAIL)
                    .authorizedScopes(authorizedScopes)
                    .attribute(Principal::class.java.name, emailAuthentication)

            val accessToken = buildAccessToken(authorizationBuilder, generatedAccessToken, tokenContext)

            // ----- Refresh token -----
            var refreshToken: OAuth2RefreshToken? = null
            // Do not issue refresh token to public client
            if (
                registeredClient.authorizationGrantTypes.contains(AuthorizationGrantType.REFRESH_TOKEN)
            ) {
                tokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.REFRESH_TOKEN).build()
                val generatedRefreshToken = tokenGenerator.generate(tokenContext)
                if (generatedRefreshToken != null) {
                    if (generatedRefreshToken !is OAuth2RefreshToken) {
                        throw OAuth2AuthenticationException(
                            OAuth2Error(
                                OAuth2ErrorCodes.SERVER_ERROR,
                                "The token generator failed to generate a valid refresh token.",
                                TOKEN_REQUEST_ERROR_URI,
                            ),
                        )
                    }

                    if (EMAIL_PROVIDER_LOGGER.isTraceEnabled) {
                        EMAIL_PROVIDER_LOGGER.trace("Generated refresh token")
                    }

                    refreshToken = generatedRefreshToken
                    authorizationBuilder.refreshToken(refreshToken)
                }
            }

            // ----- ID token -----
            var idToken: OidcIdToken? = null
            if (authorizedScopes.contains(OidcScopes.OPENID)) {
                tokenContext =
                    tokenContextBuilder
                        .tokenType(EMAIL_ID_TOKEN_TYPE)
                        // ID token customizer may need access to the access token and/or
                        // refresh token
                        .authorization(authorizationBuilder.build())
                        .build()
                val generatedIdToken = tokenGenerator.generate(tokenContext)
                if (generatedIdToken !is Jwt) {
                    throw OAuth2AuthenticationException(
                        OAuth2Error(
                            OAuth2ErrorCodes.SERVER_ERROR,
                            "The token generator failed to generate the ID token.",
                            TOKEN_REQUEST_ERROR_URI,
                        ),
                    )
                }

                if (EMAIL_PROVIDER_LOGGER.isTraceEnabled) {
                    EMAIL_PROVIDER_LOGGER.trace("Generated id token")
                }

                idToken =
                    OidcIdToken(
                        generatedIdToken.tokenValue,
                        generatedIdToken.issuedAt,
                        generatedIdToken.expiresAt,
                        generatedIdToken.claims,
                    )
                authorizationBuilder.token(idToken) { metadata ->
                    metadata[OAuth2Authorization.Token.CLAIMS_METADATA_NAME] = idToken.claims
                }
            }

            val authorization = authorizationBuilder.build()
            authorizationService.save(authorization)

            if (EMAIL_PROVIDER_LOGGER.isTraceEnabled) {
                EMAIL_PROVIDER_LOGGER.trace("Saved authorization")
            }

            val additionalParameters: Map<String, Any> =
                if (idToken != null) {
                    linkedMapOf(OidcParameterNames.ID_TOKEN to idToken.tokenValue)
                } else {
                    emptyMap()
                }

            if (EMAIL_PROVIDER_LOGGER.isTraceEnabled) {
                EMAIL_PROVIDER_LOGGER.trace("Authenticated token request")
            }

            return OAuth2AccessTokenAuthenticationToken(
                registeredClient,
                clientAuthenticationToken,
                accessToken,
                refreshToken,
                additionalParameters,
            ).also { it.details = emailGrantAuthenticationToken.details }
        }

        /** 判断当前提供者是否支持邮箱授权令牌 */
        override fun supports(authentication: Class<*>): Boolean =
            OAuth2EmailAuthorizationGrantAuthenticationToken::class
                .java
                .isAssignableFrom(authentication)
    }
