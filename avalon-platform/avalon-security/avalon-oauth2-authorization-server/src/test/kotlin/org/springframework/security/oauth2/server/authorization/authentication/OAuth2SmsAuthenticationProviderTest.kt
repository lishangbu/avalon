package org.springframework.security.oauth2.server.authorization.authentication

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import java.time.Duration

class OAuth2SmsAuthenticationProviderTest {
    @Test
    fun constructorRequiresAuthorizationServiceAndTokenGenerator() {
        val authenticationManager = Mockito.mock(AuthenticationManager::class.java)

        @Suppress("UNCHECKED_CAST")
        val tokenGenerator =
            Mockito.mock(OAuth2TokenGenerator::class.java) as OAuth2TokenGenerator<out OAuth2Token>
        val authorizationService = Mockito.mock(OAuth2AuthorizationService::class.java)

        assertThrows(IllegalArgumentException::class.java) {
            OAuth2SmsAuthenticationProvider(authenticationManager, null, tokenGenerator)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OAuth2SmsAuthenticationProvider(authenticationManager, authorizationService, null)
        }
    }

    @Test
    fun rejectsUnsupportedGrantType() {
        val registeredClient = registeredClient(AuthorizationGrantType.CLIENT_CREDENTIALS)
        val provider = provider()
        val token = smsGrantToken(registeredClient)

        val exception =
            assertThrows(OAuth2AuthenticationException::class.java) { provider.authenticate(token) }

        assertEquals(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT, exception.error.errorCode)
    }

    @Test
    fun supportsSmsGrantToken() {
        val provider = provider()

        assertTrue(provider.supports(OAuth2SmsAuthorizationGrantAuthenticationToken::class.java))
        assertTrue(!provider.supports(UsernamePasswordAuthenticationToken::class.java))
    }

    private fun provider(): OAuth2SmsAuthenticationProvider {
        val authenticationManager = Mockito.mock(AuthenticationManager::class.java)
        val authorizationService = Mockito.mock(OAuth2AuthorizationService::class.java)

        @Suppress("UNCHECKED_CAST")
        val tokenGenerator =
            Mockito.mock(OAuth2TokenGenerator::class.java) as OAuth2TokenGenerator<out OAuth2Token>
        return OAuth2SmsAuthenticationProvider(
            authenticationManager,
            authorizationService,
            tokenGenerator,
        )
    }

    private fun smsGrantToken(
        registeredClient: RegisteredClient,
    ): OAuth2SmsAuthorizationGrantAuthenticationToken {
        val clientAuthenticationToken =
            OAuth2ClientAuthenticationToken(
                registeredClient,
                ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                "secret",
            )
        return OAuth2SmsAuthorizationGrantAuthenticationToken(
            "13800000000",
            "123456",
            clientAuthenticationToken,
            emptySet(),
            mapOf(),
        )
    }

    private fun registeredClient(grantType: AuthorizationGrantType): RegisteredClient =
        RegisteredClient
            .withId("id")
            .clientId("client")
            .clientSecret("secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(grantType)
            .scope("read")
            .tokenSettings(
                TokenSettings
                    .builder()
                    .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                    .accessTokenTimeToLive(Duration.ofMinutes(5))
                    .refreshTokenTimeToLive(Duration.ofMinutes(10))
                    .build(),
            ).build()
}
