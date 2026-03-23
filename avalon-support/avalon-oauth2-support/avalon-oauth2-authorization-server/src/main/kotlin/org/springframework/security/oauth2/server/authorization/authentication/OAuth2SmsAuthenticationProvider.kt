package org.springframework.security.oauth2.server.authorization.authentication

import io.github.lishangbu.avalon.oauth2.authorizationserver.authentication.SmsAuthenticationToken
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

/** 短信授权提供者日志记录器 */
private val SMS_PROVIDER_LOGGER: Logger = LogManager.getLogger()

/** 短信 ID 令牌类型 */
private val SMS_ID_TOKEN_TYPE = OAuth2TokenType(OidcParameterNames.ID_TOKEN)

/**
 * 短信授权模式认证提供者
 *
 * 处理 `sms` 授权类型的客户端校验、短信验证码认证、令牌生成与授权保存
 */
class OAuth2SmsAuthenticationProvider
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

        /** 校验短信授权请求并签发访问令牌 */
        override fun authenticate(authentication: Authentication): Authentication {
            val smsGrantAuthenticationToken =
                authentication as OAuth2SmsAuthorizationGrantAuthenticationToken

            val clientAuthenticationToken =
                smsGrantAuthenticationToken.principal as OAuth2ClientAuthenticationToken
            val registeredClient =
                requireNotNull(clientAuthenticationToken.registeredClient) {
                    "registeredClient cannot be null"
                }

            if (SMS_PROVIDER_LOGGER.isTraceEnabled) {
                SMS_PROVIDER_LOGGER.trace("Retrieved registered client")
            }

            if (!registeredClient.authorizationGrantTypes.contains(AuthorizationGrantTypeSupport.SMS)) {
                if (SMS_PROVIDER_LOGGER.isDebugEnabled) {
                    SMS_PROVIDER_LOGGER.debug(
                        LogMessage.format(
                            "Invalid request: requested grant_type is not allowed for registered client '%s'",
                            registeredClient.id,
                        ),
                    )
                }
                throw OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT)
            }

            val phoneNumber = smsGrantAuthenticationToken.phoneNumber
            val smsCode = smsGrantAuthenticationToken.smsCode

            if (loginFailureTracker != null && loginFailureTracker.isEnabled()) {
                val remainingLock = loginFailureTracker.getRemainingLock(phoneNumber)
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

            val smsAuthenticationToken = SmsAuthenticationToken(phoneNumber, smsCode)

            val smsAuthentication =
                try {
                    authenticationManager.authenticate(smsAuthenticationToken)
                } catch (ex: AuthenticationException) {
                    loginFailureTracker?.onFailure(phoneNumber)
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

            loginFailureTracker?.onSuccess(phoneNumber)

            var authorizedScopes: Set<String> = registeredClient.scopes // Default to configured scopes
            val requestedScopes = smsGrantAuthenticationToken.scopes
            if (requestedScopes.isNotEmpty()) {
                val unauthorizedScopes = requestedScopes - registeredClient.scopes
                if (unauthorizedScopes.isNotEmpty()) {
                    throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_SCOPE)
                }

                authorizedScopes = LinkedHashSet(requestedScopes)
            }

            if (SMS_PROVIDER_LOGGER.isTraceEnabled) {
                SMS_PROVIDER_LOGGER.trace("Validated token request parameters")
            }

            val tokenContextBuilder =
                DefaultOAuth2TokenContext
                    .builder()
                    .registeredClient(registeredClient)
                    .principal(smsAuthentication)
                    .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                    .authorizedScopes(authorizedScopes)
                    .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                    .authorizationGrantType(AuthorizationGrantTypeSupport.SMS)
                    .authorizationGrant(smsGrantAuthenticationToken)

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

            if (SMS_PROVIDER_LOGGER.isTraceEnabled) {
                SMS_PROVIDER_LOGGER.trace("Generated access token")
            }

            val authorizationBuilder =
                OAuth2Authorization
                    .withRegisteredClient(registeredClient)
                    .principalName(phoneNumber)
                    .authorizationGrantType(AuthorizationGrantTypeSupport.SMS)
                    .authorizedScopes(authorizedScopes)
                    .attribute(Principal::class.java.name, smsAuthentication)

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

                    if (SMS_PROVIDER_LOGGER.isTraceEnabled) {
                        SMS_PROVIDER_LOGGER.trace("Generated refresh token")
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
                        .tokenType(SMS_ID_TOKEN_TYPE)
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

                if (SMS_PROVIDER_LOGGER.isTraceEnabled) {
                    SMS_PROVIDER_LOGGER.trace("Generated id token")
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

            if (SMS_PROVIDER_LOGGER.isTraceEnabled) {
                SMS_PROVIDER_LOGGER.trace("Saved authorization")
            }

            val additionalParameters: Map<String, Any> =
                if (idToken != null) {
                    linkedMapOf(OidcParameterNames.ID_TOKEN to idToken.tokenValue)
                } else {
                    emptyMap()
                }

            if (SMS_PROVIDER_LOGGER.isTraceEnabled) {
                SMS_PROVIDER_LOGGER.trace("Authenticated token request")
            }

            return OAuth2AccessTokenAuthenticationToken(
                registeredClient,
                clientAuthenticationToken,
                accessToken,
                refreshToken,
                additionalParameters,
            ).also { it.details = smsGrantAuthenticationToken.details }
        }

        /** 判断当前提供者是否支持短信授权令牌 */
        override fun supports(authentication: Class<*>): Boolean =
            OAuth2SmsAuthorizationGrantAuthenticationToken::class
                .java
                .isAssignableFrom(authentication)
    }
