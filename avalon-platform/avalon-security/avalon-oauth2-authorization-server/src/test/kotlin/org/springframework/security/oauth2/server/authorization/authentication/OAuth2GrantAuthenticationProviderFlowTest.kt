package org.springframework.security.oauth2.server.authorization.authentication

import io.github.lishangbu.avalon.oauth2.authorizationserver.authentication.EmailAuthenticationToken
import io.github.lishangbu.avalon.oauth2.authorizationserver.authentication.SmsAuthenticationToken
import io.github.lishangbu.avalon.oauth2.authorizationserver.login.LoginFailureTracker
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.OAuth2Token
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import java.time.Duration
import java.time.Instant

class OAuth2GrantAuthenticationProviderFlowTest {
    @AfterEach
    fun resetAuthorizationServerContext() {
        AuthorizationServerContextHolder.resetContext()
    }

    @ParameterizedTest
    @EnumSource(GrantKind::class)
    internal fun authenticatesAndSavesAuthorization(kind: GrantKind) {
        val authenticationManager = mock(AuthenticationManager::class.java)
        val authorizationService = mock(OAuth2AuthorizationService::class.java)
        val loginFailureTracker = mock(LoginFailureTracker::class.java)
        val tokenGenerator = tokenGenerator()
        val issuedAt = Instant.parse("2026-03-25T00:00:00Z")
        val expiresAt = issuedAt.plusSeconds(300)
        val refreshExpiresAt = expiresAt.plusSeconds(300)
        var savedAuthorization: OAuth2Authorization? = null

        `when`(loginFailureTracker.isEnabled()).thenReturn(true)
        `when`(loginFailureTracker.getRemainingLock(kind.subject)).thenReturn(null)
        Mockito
            .doAnswer {
                val authentication = it.getArgument(0, Authentication::class.java)
                assertTrue(kind.authenticationClass.isInstance(authentication))
                assertEquals(kind.subject, authentication.name)
                kind.authenticatedPrincipal()
            }.`when`(authenticationManager)
            .authenticate(any())
        Mockito
            .doAnswer {
                savedAuthorization = it.getArgument(0)
                null
            }.`when`(authorizationService)
            .save(any())
        Mockito
            .doAnswer {
                val context = it.getArgument(0, OAuth2TokenContext::class.java)
                when (context.tokenType.value) {
                    OAuth2ParameterNames.ACCESS_TOKEN -> {
                        OAuth2AccessToken(
                            OAuth2AccessToken.TokenType.BEARER,
                            "access-token",
                            issuedAt,
                            expiresAt,
                            context.authorizedScopes,
                        )
                    }

                    OAuth2ParameterNames.REFRESH_TOKEN -> {
                        OAuth2RefreshToken("refresh-token", issuedAt, refreshExpiresAt)
                    }

                    OidcParameterNames.ID_TOKEN -> {
                        Jwt
                            .withTokenValue("id-token")
                            .header("alg", "RS256")
                            .claim("sub", kind.subject)
                            .issuedAt(issuedAt)
                            .expiresAt(expiresAt)
                            .build()
                    }

                    else -> {
                        null
                    }
                }
            }.`when`(tokenGenerator)
            .generate(any())

        val provider =
            kind.createProvider(
                authenticationManager = authenticationManager,
                authorizationService = authorizationService,
                tokenGenerator = tokenGenerator,
                loginFailureTracker = loginFailureTracker,
            )
        val request = kind.request(registeredClient(kind.grantType), setOf("openid", "read"))
        request.details = "request-details"

        val result =
            withAuthorizationServerContext {
                provider.authenticate(request) as OAuth2AccessTokenAuthenticationToken
            }

        assertEquals("request-details", result.details)
        assertEquals("access-token", result.accessToken!!.tokenValue)
        assertEquals(setOf("openid", "read"), result.accessToken!!.scopes)
        assertEquals("refresh-token", result.refreshToken!!.tokenValue)
        assertEquals("id-token", result.additionalParameters[OidcParameterNames.ID_TOKEN])

        verify(loginFailureTracker).onSuccess(kind.subject)

        val authorization = requireNotNull(savedAuthorization)
        assertEquals(kind.subject, authorization.principalName)
        assertEquals(kind.grantType.value, authorization.authorizationGrantType.value)
        assertEquals(setOf("openid", "read"), authorization.authorizedScopes)
        assertEquals("access-token", authorization.getToken(OAuth2AccessToken::class.java)!!.token.tokenValue)
        assertEquals("refresh-token", authorization.getToken(OAuth2RefreshToken::class.java)!!.token.tokenValue)
        assertEquals("id-token", authorization.getToken(OidcIdToken::class.java)!!.token.tokenValue)
    }

    @ParameterizedTest
    @EnumSource(GrantKind::class)
    internal fun rejectsLockedPrincipalBeforeAuthentication(kind: GrantKind) {
        val authenticationManager = mock(AuthenticationManager::class.java)
        val authorizationService = mock(OAuth2AuthorizationService::class.java)
        val loginFailureTracker = mock(LoginFailureTracker::class.java)
        val tokenGenerator = tokenGenerator()

        `when`(loginFailureTracker.isEnabled()).thenReturn(true)
        `when`(loginFailureTracker.getRemainingLock(kind.subject)).thenReturn(Duration.ofSeconds(65))

        val provider =
            kind.createProvider(
                authenticationManager = authenticationManager,
                authorizationService = authorizationService,
                tokenGenerator = tokenGenerator,
                loginFailureTracker = loginFailureTracker,
            )
        val request = kind.request(registeredClient(kind.grantType), setOf("read"))

        val exception =
            withAuthorizationServerContext {
                assertThrows(OAuth2AuthenticationException::class.java) {
                    provider.authenticate(request)
                }
            }

        assertEquals(OAuth2ErrorCodes.INVALID_GRANT, exception.error.errorCode)
        assertEquals("账号已被锁定，请在2分钟后重试", exception.error.description)
        verify(loginFailureTracker, never()).onFailure(any())
        verify(loginFailureTracker, never()).onSuccess(any())
        verifyNoInteractions(authenticationManager)
    }

    @ParameterizedTest
    @EnumSource(GrantKind::class)
    internal fun wrapsAuthenticationFailuresAsInvalidGrant(kind: GrantKind) {
        val authenticationManager = mock(AuthenticationManager::class.java)
        val authorizationService = mock(OAuth2AuthorizationService::class.java)
        val loginFailureTracker = mock(LoginFailureTracker::class.java)
        val tokenGenerator = tokenGenerator()

        `when`(loginFailureTracker.isEnabled()).thenReturn(true)
        `when`(loginFailureTracker.getRemainingLock(kind.subject)).thenReturn(null)
        `when`(authenticationManager.authenticate(any()))
            .thenThrow(BadCredentialsException("bad credentials"))

        val provider =
            kind.createProvider(
                authenticationManager = authenticationManager,
                authorizationService = authorizationService,
                tokenGenerator = tokenGenerator,
                loginFailureTracker = loginFailureTracker,
            )
        val request = kind.request(registeredClient(kind.grantType), setOf("read"))

        val exception =
            withAuthorizationServerContext {
                assertThrows(OAuth2AuthenticationException::class.java) {
                    provider.authenticate(request)
                }
            }

        assertEquals(OAuth2ErrorCodes.INVALID_GRANT, exception.error.errorCode)
        assertEquals("bad credentials", exception.error.description)
        verify(loginFailureTracker).onFailure(kind.subject)
    }

    @ParameterizedTest
    @EnumSource(GrantKind::class)
    internal fun rethrowsOAuth2AuthenticationExceptionFromAuthenticationManager(kind: GrantKind) {
        val authenticationManager = mock(AuthenticationManager::class.java)
        val authorizationService = mock(OAuth2AuthorizationService::class.java)
        val loginFailureTracker = mock(LoginFailureTracker::class.java)
        val tokenGenerator = tokenGenerator()
        val original =
            OAuth2AuthenticationException(
                OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "original", TOKEN_REQUEST_ERROR_URI),
            )

        `when`(loginFailureTracker.isEnabled()).thenReturn(true)
        `when`(loginFailureTracker.getRemainingLock(kind.subject)).thenReturn(null)
        `when`(authenticationManager.authenticate(any())).thenThrow(original)

        val provider =
            kind.createProvider(
                authenticationManager = authenticationManager,
                authorizationService = authorizationService,
                tokenGenerator = tokenGenerator,
                loginFailureTracker = loginFailureTracker,
            )
        val request = kind.request(registeredClient(kind.grantType), setOf("read"))

        val exception =
            withAuthorizationServerContext {
                assertThrows(OAuth2AuthenticationException::class.java) {
                    provider.authenticate(request)
                }
            }

        assertSame(original, exception)
        verify(loginFailureTracker).onFailure(kind.subject)
    }

    @ParameterizedTest
    @EnumSource(GrantKind::class)
    internal fun rejectsUnauthorizedScopes(kind: GrantKind) {
        val authenticationManager = mock(AuthenticationManager::class.java)
        val authorizationService = mock(OAuth2AuthorizationService::class.java)
        val loginFailureTracker = mock(LoginFailureTracker::class.java)
        val tokenGenerator = tokenGenerator()

        `when`(loginFailureTracker.isEnabled()).thenReturn(true)
        `when`(loginFailureTracker.getRemainingLock(kind.subject)).thenReturn(null)
        Mockito
            .doAnswer { kind.authenticatedPrincipal() }
            .`when`(authenticationManager)
            .authenticate(any())

        val provider =
            kind.createProvider(
                authenticationManager = authenticationManager,
                authorizationService = authorizationService,
                tokenGenerator = tokenGenerator,
                loginFailureTracker = loginFailureTracker,
            )
        val request = kind.request(registeredClient(kind.grantType), setOf("write"))

        val exception =
            withAuthorizationServerContext {
                assertThrows(OAuth2AuthenticationException::class.java) {
                    provider.authenticate(request)
                }
            }

        assertEquals(OAuth2ErrorCodes.INVALID_SCOPE, exception.error.errorCode)
        verify(loginFailureTracker).onSuccess(kind.subject)
        verifyNoInteractions(tokenGenerator)
        verify(authorizationService, never()).save(any())
    }

    internal enum class GrantKind(
        val grantType: AuthorizationGrantType,
        val subject: String,
        val authenticationClass: Class<out Authentication>,
    ) {
        PASSWORD(
            grantType = AuthorizationGrantTypeSupport.PASSWORD,
            subject = "user",
            authenticationClass = UsernamePasswordAuthenticationToken::class.java,
        ) {
            override fun request(
                registeredClient: RegisteredClient,
                scopes: Set<String>,
            ): OAuth2AuthorizationGrantAuthenticationToken =
                OAuth2PasswordAuthorizationGrantAuthenticationToken(
                    subject,
                    "password",
                    clientAuthenticationToken(registeredClient),
                    scopes,
                    emptyMap(),
                )

            override fun authenticatedPrincipal(): Authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                    subject,
                    "password",
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                )

            override fun createProvider(
                authenticationManager: AuthenticationManager,
                authorizationService: OAuth2AuthorizationService,
                tokenGenerator: OAuth2TokenGenerator<out OAuth2Token>,
                loginFailureTracker: LoginFailureTracker,
            ): AuthenticationProvider =
                OAuth2PasswordAuthenticationProvider(
                    authenticationManager,
                    authorizationService,
                    tokenGenerator,
                    loginFailureTracker,
                )
        },
        SMS(
            grantType = AuthorizationGrantTypeSupport.SMS,
            subject = "13800000000",
            authenticationClass = SmsAuthenticationToken::class.java,
        ) {
            override fun request(
                registeredClient: RegisteredClient,
                scopes: Set<String>,
            ): OAuth2AuthorizationGrantAuthenticationToken =
                OAuth2SmsAuthorizationGrantAuthenticationToken(
                    subject,
                    "123456",
                    clientAuthenticationToken(registeredClient),
                    scopes,
                    emptyMap(),
                )

            override fun authenticatedPrincipal(): Authentication =
                SmsAuthenticationToken(
                    subject,
                    "123456",
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                )

            override fun createProvider(
                authenticationManager: AuthenticationManager,
                authorizationService: OAuth2AuthorizationService,
                tokenGenerator: OAuth2TokenGenerator<out OAuth2Token>,
                loginFailureTracker: LoginFailureTracker,
            ): AuthenticationProvider =
                OAuth2SmsAuthenticationProvider(
                    authenticationManager,
                    authorizationService,
                    tokenGenerator,
                    loginFailureTracker,
                )
        },
        EMAIL(
            grantType = AuthorizationGrantTypeSupport.EMAIL,
            subject = "user@example.com",
            authenticationClass = EmailAuthenticationToken::class.java,
        ) {
            override fun request(
                registeredClient: RegisteredClient,
                scopes: Set<String>,
            ): OAuth2AuthorizationGrantAuthenticationToken =
                OAuth2EmailAuthorizationGrantAuthenticationToken(
                    subject,
                    "123456",
                    clientAuthenticationToken(registeredClient),
                    scopes,
                    emptyMap(),
                )

            override fun authenticatedPrincipal(): Authentication =
                EmailAuthenticationToken(
                    subject,
                    "123456",
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                )

            override fun createProvider(
                authenticationManager: AuthenticationManager,
                authorizationService: OAuth2AuthorizationService,
                tokenGenerator: OAuth2TokenGenerator<out OAuth2Token>,
                loginFailureTracker: LoginFailureTracker,
            ): AuthenticationProvider =
                OAuth2EmailAuthenticationProvider(
                    authenticationManager,
                    authorizationService,
                    tokenGenerator,
                    loginFailureTracker,
                )
        },
        ;

        abstract fun request(
            registeredClient: RegisteredClient,
            scopes: Set<String>,
        ): OAuth2AuthorizationGrantAuthenticationToken

        abstract fun authenticatedPrincipal(): Authentication

        abstract fun createProvider(
            authenticationManager: AuthenticationManager,
            authorizationService: OAuth2AuthorizationService,
            tokenGenerator: OAuth2TokenGenerator<out OAuth2Token>,
            loginFailureTracker: LoginFailureTracker,
        ): AuthenticationProvider
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun tokenGenerator(): OAuth2TokenGenerator<out OAuth2Token> = mock(OAuth2TokenGenerator::class.java) as OAuth2TokenGenerator<out OAuth2Token>

        private fun registeredClient(grantType: AuthorizationGrantType): RegisteredClient =
            RegisteredClient
                .withId("id")
                .clientId("client")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(grantType)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .scope("read")
                .scope(OidcScopes.OPENID)
                .tokenSettings(
                    TokenSettings
                        .builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                        .accessTokenTimeToLive(Duration.ofMinutes(5))
                        .refreshTokenTimeToLive(Duration.ofMinutes(10))
                        .build(),
                ).build()

        private fun clientAuthenticationToken(
            registeredClient: RegisteredClient,
        ): OAuth2ClientAuthenticationToken =
            OAuth2ClientAuthenticationToken(
                registeredClient,
                ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                "secret",
            )

        private fun <T> withAuthorizationServerContext(block: () -> T): T {
            AuthorizationServerContextHolder.setContext(
                object : AuthorizationServerContext {
                    override fun getIssuer(): String = "https://issuer.example"

                    override fun getAuthorizationServerSettings(): AuthorizationServerSettings =
                        AuthorizationServerSettings
                            .builder()
                            .issuer("https://issuer.example")
                            .build()
                },
            )
            return try {
                block()
            } finally {
                AuthorizationServerContextHolder.resetContext()
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> any(): T = Mockito.any<T>()
    }
}
