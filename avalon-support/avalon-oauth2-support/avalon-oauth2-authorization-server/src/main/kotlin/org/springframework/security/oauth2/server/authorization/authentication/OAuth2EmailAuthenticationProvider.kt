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
import org.springframework.util.Assert
import org.springframework.util.CollectionUtils
import java.security.Principal

/**
 * OAuth2 邮箱授权模式的认证提供者
 *
 * 负责处理邮箱验证码授权类型的令牌申请 校验客户端授权类型、邮箱验证码，以及生成并保存授权信息
 *
 * @author lishangbu
 * @since 2026/3/13
 */
private val EMAIL_PROVIDER_LOGGER: Logger = LogManager.getLogger()
private val EMAIL_ID_TOKEN_TYPE = OAuth2TokenType(OidcParameterNames.ID_TOKEN)

class OAuth2EmailAuthenticationProvider
    @JvmOverloads
    constructor(
        private val authenticationManager: AuthenticationManager,
        authorizationService: OAuth2AuthorizationService?,
        tokenGenerator: OAuth2TokenGenerator<out OAuth2Token>?,
        private val loginFailureTracker: LoginFailureTracker? = null,
    ) : AuthenticationProvider {
        private val authorizationService: OAuth2AuthorizationService
        private val tokenGenerator: OAuth2TokenGenerator<out OAuth2Token>

        init {
            Assert.notNull(authorizationService, "authorizationService cannot be null")
            Assert.notNull(tokenGenerator, "tokenGenerator cannot be null")
            this.authorizationService = authorizationService!!
            this.tokenGenerator = tokenGenerator!!
        }

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
            if (!CollectionUtils.isEmpty(requestedScopes)) {
                val unauthorizedScopes =
                    requestedScopes.filterNot { registeredClient.scopes.contains(it) }.toSet()
                if (!CollectionUtils.isEmpty(unauthorizedScopes)) {
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

        override fun supports(authentication: Class<*>): Boolean =
            OAuth2EmailAuthorizationGrantAuthenticationToken::class
                .java
                .isAssignableFrom(authentication)
    }
