package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.repository.Oauth2RegisteredClientRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import java.time.Duration

class DefaultRegisteredClientRepositoryTest {
    private val repository = mock(Oauth2RegisteredClientRepository::class.java)
    private val adapter = DefaultRegisteredClientRepository(repository)

    @Test
    fun saveMapsRegisteredClientIntoEntity() {
        var persisted: io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient? = null
        Mockito
            .doAnswer {
                persisted = it.getArgument(0)
                persisted
            }.`when`(repository)
            .save(any())

        adapter.save(registeredClient())

        val saved = requireNotNull(persisted)
        assertEquals("client-id", saved.id)
        assertEquals("client", saved.clientId)
        assertEquals(
            setOf("client_secret_basic", "private_key_jwt"),
            requireNotNull(saved.clientAuthenticationMethods).split(',').toSet(),
        )
        assertEquals(
            setOf("authorization_code", "refresh_token", "urn:custom:grant"),
            requireNotNull(saved.authorizationGrantTypes).split(',').toSet(),
        )
        assertEquals(
            setOf("https://app.example/callback", "https://app.example/alt"),
            requireNotNull(saved.redirectUris).split(',').toSet(),
        )
        assertEquals("https://app.example/logout", saved.postLogoutRedirectUris)
        assertEquals(setOf("openid", "profile"), requireNotNull(saved.scopes).split(',').toSet())
        assertEquals(true, saved.requireProofKey)
        assertEquals(false, saved.requireAuthorizationConsent)
        assertEquals("HS256", saved.tokenEndpointAuthenticationSigningAlgorithm)
        assertEquals("https://issuer.example/jwks", saved.jwkSetUrl)
        assertEquals("CN=client", saved.x509CertificateSubjectDn)
        assertEquals("PT1M", saved.authorizationCodeTimeToLive)
        assertEquals("PT2M", saved.accessTokenTimeToLive)
        assertEquals("reference", saved.accessTokenFormat)
        assertEquals("PT3M", saved.deviceCodeTimeToLive)
        assertEquals(false, saved.reuseRefreshTokens)
        assertEquals("PT4M", saved.refreshTokenTimeToLive)
        assertEquals("RS256", saved.idTokenSignatureAlgorithm)
        assertEquals(true, saved.x509CertificateBoundAccessTokens)
    }

    @Test
    fun findByIdRejectsBlankId() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                adapter.findById(" ")
            }

        assertEquals("id cannot be empty", exception.message)
    }

    @Test
    fun findByClientIdRejectsBlankClientId() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                adapter.findByClientId(" ")
            }

        assertEquals("clientId cannot be empty", exception.message)
    }

    @Test
    fun findByIdReturnsNullWhenRepositoryMisses() {
        `when`(repository.findNullable("missing")).thenReturn(null)

        val found = adapter.findById("missing")

        assertNull(found)
    }

    @Test
    fun findByClientIdReturnsNullWhenRepositoryMisses() {
        `when`(repository.findByClientId("missing-client")).thenReturn(null)

        val found = adapter.findByClientId("missing-client")

        assertNull(found)
    }

    @Test
    fun findByIdMapsEntityUsingMacAlgorithm() {
        `when`(repository.findNullable("client-id")).thenReturn(
            registeredClientEntity(
                id = "client-id",
                tokenEndpointAuthenticationSigningAlgorithm = "HS256",
            ),
        )

        val found = adapter.findById("client-id")

        assertNotNull(found)
        assertEquals("client-id", found!!.id)
        assertEquals(setOf("client_secret_basic", "private_key_jwt"), found.clientAuthenticationMethods.map { it.value }.toSet())
        assertEquals(setOf("authorization_code", "refresh_token", "urn:custom:grant"), found.authorizationGrantTypes.map { it.value }.toSet())
        assertEquals(setOf("https://app.example/callback", "https://app.example/alt"), found.redirectUris)
        assertEquals(setOf("openid", "profile"), found.scopes)
        assertEquals(MacAlgorithm.HS256.name, found.clientSettings.tokenEndpointAuthenticationSigningAlgorithm!!.name)
        assertEquals(Duration.ofMinutes(2), found.tokenSettings.accessTokenTimeToLive)
        assertEquals(OAuth2TokenFormat.REFERENCE.value, found.tokenSettings.accessTokenFormat.value)
    }

    @Test
    fun findByClientIdMapsEntityUsingSignatureAlgorithm() {
        `when`(repository.findByClientId("client")).thenReturn(
            registeredClientEntity(
                id = "client-id",
                tokenEndpointAuthenticationSigningAlgorithm = "RS256",
            ),
        )

        val found = adapter.findByClientId("client")

        assertNotNull(found)
        assertEquals(SignatureAlgorithm.RS256.name, found!!.clientSettings.tokenEndpointAuthenticationSigningAlgorithm!!.name)
        assertEquals(SignatureAlgorithm.RS256.name, found.tokenSettings.idTokenSignatureAlgorithm!!.name)
    }

    @Test
    fun findByIdMapsKnownGrantAndAuthenticationMethodVariants() {
        `when`(repository.findNullable("variant-client")).thenReturn(
            registeredClientEntity(
                id = "variant-client",
                tokenEndpointAuthenticationSigningAlgorithm = null,
                clientAuthenticationMethods = "client_secret_post,none",
                authorizationGrantTypes = "client_credentials",
                postLogoutRedirectUris = null,
                requireProofKey = null,
                requireAuthorizationConsent = null,
                jwkSetUrl = null,
                x509CertificateSubjectDn = null,
                authorizationCodeTimeToLive = null,
                accessTokenTimeToLive = null,
                accessTokenFormat = null,
                deviceCodeTimeToLive = null,
                reuseRefreshTokens = null,
                refreshTokenTimeToLive = null,
                idTokenSignatureAlgorithm = null,
                x509CertificateBoundAccessTokens = null,
            ),
        )

        val found = requireNotNull(adapter.findById("variant-client"))

        assertEquals(
            setOf(
                ClientAuthenticationMethod.CLIENT_SECRET_POST.value,
                ClientAuthenticationMethod.NONE.value,
            ),
            found.clientAuthenticationMethods.map { it.value }.toSet(),
        )
        assertEquals(
            setOf(AuthorizationGrantType.CLIENT_CREDENTIALS.value),
            found.authorizationGrantTypes.map { it.value }.toSet(),
        )
        assertEquals(emptySet<String>(), found.postLogoutRedirectUris)
        assertNull(found.clientSettings.tokenEndpointAuthenticationSigningAlgorithm)
    }

    @Test
    fun savePersistsFrameworkDefaultSettingsWhenClientUsesDefaults() {
        var persisted: io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient? = null
        Mockito
            .doAnswer {
                persisted = it.getArgument(0)
                persisted
            }.`when`(repository)
            .save(any())

        val minimalClient =
            org.springframework.security.oauth2.server.authorization.client.RegisteredClient
                .withId("minimal-id")
                .clientId("minimal-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build()

        adapter.save(minimalClient)

        val saved = requireNotNull(persisted)
        val clientSettings = minimalClient.clientSettings
        val tokenSettings = minimalClient.tokenSettings
        assertEquals("minimal-id", saved.id)
        assertEquals(ClientAuthenticationMethod.NONE.value, saved.clientAuthenticationMethods)
        assertEquals(AuthorizationGrantType.CLIENT_CREDENTIALS.value, saved.authorizationGrantTypes)
        assertNull(saved.clientSecret)
        assertEquals("", saved.postLogoutRedirectUris)
        assertEquals(clientSettings.isRequireProofKey, saved.requireProofKey)
        assertEquals(clientSettings.isRequireAuthorizationConsent, saved.requireAuthorizationConsent)
        assertNull(saved.tokenEndpointAuthenticationSigningAlgorithm)
        assertEquals(tokenSettings.authorizationCodeTimeToLive.toString(), saved.authorizationCodeTimeToLive)
        assertEquals(tokenSettings.accessTokenTimeToLive.toString(), saved.accessTokenTimeToLive)
        assertEquals(tokenSettings.accessTokenFormat.value, saved.accessTokenFormat)
        assertEquals(tokenSettings.deviceCodeTimeToLive.toString(), saved.deviceCodeTimeToLive)
        assertEquals(tokenSettings.isReuseRefreshTokens, saved.reuseRefreshTokens)
        assertEquals(tokenSettings.refreshTokenTimeToLive.toString(), saved.refreshTokenTimeToLive)
        assertEquals(tokenSettings.idTokenSignatureAlgorithm.name, saved.idTokenSignatureAlgorithm)
    }

    @Test
    fun findByIdIgnoresUnknownSigningAlgorithms() {
        `when`(repository.findNullable("unknown-alg-client")).thenReturn(
            registeredClientEntity(
                id = "unknown-alg-client",
                tokenEndpointAuthenticationSigningAlgorithm = "unknown-alg",
                idTokenSignatureAlgorithm = "unknown-alg",
            ),
        )

        val found = requireNotNull(adapter.findById("unknown-alg-client"))

        assertNull(found.clientSettings.tokenEndpointAuthenticationSigningAlgorithm)
        assertEquals(SignatureAlgorithm.RS256.name, found.tokenSettings.idTokenSignatureAlgorithm!!.name)
    }
}
