package org.springframework.security.oauth2.server.authorization.authentication

import io.github.lishangbu.avalon.oauth2.authorizationserver.login.LoginFailureTracker
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.core.log.LogMessage
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
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
import java.security.Principal

private val PASSWORD_PROVIDER_LOGGER: Logger = LogManager.getLogger()
private val PASSWORD_ID_TOKEN_TYPE = OAuth2TokenType(OidcParameterNames.ID_TOKEN)

/**
 * OAuth2 Password 授权模式的认证提供者
 *
 * 负责处理 Resource Owner Password Credentials 授权类型的令牌申请 校验客户端授权类型、用户名密码凭证，以及生成并保存授权信息
 *
 * @param authenticationManager 认证管理器
 * @param authorizationService 授权服务
 * @param tokenGenerator 令牌生成器
 * @see OAuth2AuthorizationCodeAuthenticationProvider
 * @see OAuth2RefreshTokenAuthenticationProvider
 * @see OAuth2ClientCredentialsAuthenticationProvider
 * @see UserInfo
 * @see io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo
 * @since 2025/9/29
 *
 * 构造方法
 *
 * 构造一个 OAuth2PasswordAuthenticationProvider 实例，注入必要组件
 *
 * @since 0.2.3
 */
class OAuth2PasswordAuthenticationProvider
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
            val passwordGrantAuthenticationToken =
                authentication as OAuth2PasswordAuthorizationGrantAuthenticationToken

            val clientAuthenticationToken =
                passwordGrantAuthenticationToken.principal as OAuth2ClientAuthenticationToken
            val registeredClient =
                requireNotNull(clientAuthenticationToken.registeredClient) {
                    "registeredClient cannot be null"
                }

            if (PASSWORD_PROVIDER_LOGGER.isTraceEnabled) {
                PASSWORD_PROVIDER_LOGGER.trace("Retrieved registered client")
            }

            if (
                !registeredClient.authorizationGrantTypes.contains(
                    AuthorizationGrantTypeSupport.PASSWORD,
                )
            ) {
                if (PASSWORD_PROVIDER_LOGGER.isDebugEnabled) {
                    PASSWORD_PROVIDER_LOGGER.debug(
                        LogMessage.format(
                            "Invalid request: requested grant_type is not allowed for registered client '%s'",
                            registeredClient.id,
                        ),
                    )
                }
                throw OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT)
            }

            val username = passwordGrantAuthenticationToken.username
            val password = passwordGrantAuthenticationToken.password

            if (loginFailureTracker != null && loginFailureTracker.isEnabled()) {
                val remainingLock = loginFailureTracker.getRemainingLock(username)
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

            val usernamePasswordAuthenticationToken =
                UsernamePasswordAuthenticationToken(username, password)
            PASSWORD_PROVIDER_LOGGER.debug(
                "got usernamePasswordAuthenticationToken=$usernamePasswordAuthenticationToken",
            )

            val usernamePasswordAuthentication =
                try {
                    authenticationManager.authenticate(usernamePasswordAuthenticationToken)
                } catch (ex: AuthenticationException) {
                    loginFailureTracker?.onFailure(username)
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

            loginFailureTracker?.onSuccess(username)

            var authorizedScopes: Set<String> = registeredClient.scopes // Default to configured scopes
            val requestedScopes = passwordGrantAuthenticationToken.scopes
            if (requestedScopes.isNotEmpty()) {
                val unauthorizedScopes =
                    requestedScopes.filterNot { registeredClient.scopes.contains(it) }.toSet()
                if (unauthorizedScopes.isNotEmpty()) {
                    throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_SCOPE)
                }
                authorizedScopes = LinkedHashSet(requestedScopes)
            }

            if (PASSWORD_PROVIDER_LOGGER.isTraceEnabled) {
                PASSWORD_PROVIDER_LOGGER.trace("Validated token request parameters")
            }

            // @formatter:off
            val tokenContextBuilder =
                DefaultOAuth2TokenContext
                    .builder()
                    .registeredClient(registeredClient)
                    .principal(usernamePasswordAuthentication)
                    .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                    .authorizedScopes(authorizedScopes)
                    .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                    .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
                    .authorizationGrant(passwordGrantAuthenticationToken)
            // @formatter:on

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

            if (PASSWORD_PROVIDER_LOGGER.isTraceEnabled) {
                PASSWORD_PROVIDER_LOGGER.trace("Generated access token")
            }

            val authorizationBuilder =
                OAuth2Authorization
                    .withRegisteredClient(registeredClient)
                    .principalName(username)
                    .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
                    .authorizedScopes(authorizedScopes)
                    .attribute(Principal::class.java.name, usernamePasswordAuthentication)

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

                    if (PASSWORD_PROVIDER_LOGGER.isTraceEnabled) {
                        PASSWORD_PROVIDER_LOGGER.trace("Generated refresh token")
                    }

                    refreshToken = generatedRefreshToken
                    authorizationBuilder.refreshToken(refreshToken)
                }
            }

            // ----- ID token -----
            var idToken: OidcIdToken? = null
            if (authorizedScopes.contains(OidcScopes.OPENID)) {
                // @formatter:off
                tokenContext =
                    tokenContextBuilder
                        .tokenType(PASSWORD_ID_TOKEN_TYPE)
                        // ID token customizer may need access to the access token and/or
                        // refresh token
                        .authorization(authorizationBuilder.build())
                        .build()
                // @formatter:on
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

                if (PASSWORD_PROVIDER_LOGGER.isTraceEnabled) {
                    PASSWORD_PROVIDER_LOGGER.trace("Generated id token")
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

            if (PASSWORD_PROVIDER_LOGGER.isTraceEnabled) {
                PASSWORD_PROVIDER_LOGGER.trace("Saved authorization")
            }

            val additionalParameters: Map<String, Any> =
                if (idToken != null) {
                    linkedMapOf(OidcParameterNames.ID_TOKEN to idToken.tokenValue)
                } else {
                    emptyMap()
                }

            if (PASSWORD_PROVIDER_LOGGER.isTraceEnabled) {
                // This log is kept separate for consistency with other providers
                PASSWORD_PROVIDER_LOGGER.trace("Authenticated token request")
            }

            return OAuth2AccessTokenAuthenticationToken(
                registeredClient,
                clientAuthenticationToken,
                accessToken,
                refreshToken,
                additionalParameters,
            ).also { it.details = passwordGrantAuthenticationToken.details }
        }

        override fun supports(authentication: Class<*>): Boolean =
            OAuth2PasswordAuthorizationGrantAuthenticationToken::class
                .java
                .isAssignableFrom(authentication)
    }
