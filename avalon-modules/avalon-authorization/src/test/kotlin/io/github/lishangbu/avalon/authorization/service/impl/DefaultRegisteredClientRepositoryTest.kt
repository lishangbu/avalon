package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.repository.Oauth2RegisteredClientRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
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
    fun findByIdMapsEntityUsingMacAlgorithm() {
        `when`(repository.findById("client-id")).thenReturn(
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
}
