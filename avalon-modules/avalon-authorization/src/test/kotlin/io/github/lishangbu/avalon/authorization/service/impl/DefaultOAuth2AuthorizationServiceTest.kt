package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.repository.Oauth2AuthorizationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2DeviceCode
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.OAuth2UserCode
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import java.time.Instant

class DefaultOAuth2AuthorizationServiceTest {
    private val authorizationRepository = mock(Oauth2AuthorizationRepository::class.java)
    private val registeredClientRepository = mock(RegisteredClientRepository::class.java)
    private val service =
        DefaultOAuth2AuthorizationService(
            authorizationRepository,
            registeredClientRepository,
        )

    @Test
    fun saveMapsAuthorizationIntoEntity() {
        var persisted: io.github.lishangbu.avalon.authorization.entity.OauthAuthorization? = null
        Mockito
            .doAnswer {
                persisted = it.getArgument(0)
                persisted
            }.`when`(authorizationRepository)
            .save(any())

        service.save(oauth2Authorization())

        val entity = requireNotNull(persisted)
        assertEquals("authorization-id", entity.id)
        assertEquals("client-id", entity.registeredClientId)
        assertEquals("alice", entity.principalName)
        assertEquals(io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport.PASSWORD.value, entity.authorizationGrantType)
        assertEquals(setOf("openid", "read"), entity.authorizedScopes!!.split(',').toSet())
        assertTrue(entity.attributes!!.contains("\"custom\":\"value\""))
        assertEquals("state-token", entity.state)
        assertEquals("auth-code", entity.authorizationCodeValue)
        assertEquals("access-token", entity.accessTokenValue)
        assertEquals("Bearer", entity.accessTokenType)
        assertEquals(setOf("openid", "read"), entity.accessTokenScopes!!.split(',').toSet())
        assertEquals("refresh-token", entity.refreshTokenValue)
        assertEquals("id-token", entity.oidcIdTokenValue)
        assertEquals("user-code", entity.userCodeValue)
        assertEquals("device-code", entity.deviceCodeValue)
    }

    @Test
    fun removeDelegatesDeleteToRepository() {
        service.remove(oauth2Authorization())

        verify(authorizationRepository).deleteById("authorization-id")
    }

    @Test
    fun findByIdRejectsBlankId() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                service.findById(" ")
            }

        assertEquals("id cannot be empty", exception.message)
    }

    @Test
    fun findByIdReturnsNullWhenRepositoryMisses() {
        `when`(authorizationRepository.findById("authorization-id")).thenReturn(null)

        assertNull(service.findById("authorization-id"))
    }

    @Test
    fun findByIdThrowsWhenRegisteredClientIsMissing() {
        `when`(authorizationRepository.findById("authorization-id")).thenReturn(authorizationEntity())
        `when`(registeredClientRepository.findById("client-id")).thenReturn(null)

        assertThrows(DataRetrievalFailureException::class.java) {
            service.findById("authorization-id")
        }
    }

    @Test
    fun findByIdMapsEntityBackToAuthorization() {
        val entity = roundTripAuthorizationEntity()
        `when`(authorizationRepository.findById("authorization-id")).thenReturn(entity)
        `when`(registeredClientRepository.findById("client-id")).thenReturn(registeredClient())

        val authorization = service.findById("authorization-id")

        assertNotNull(authorization)
        assertEquals("authorization-id", authorization!!.id)
        assertEquals("client-id", authorization.registeredClientId)
        assertEquals("alice", authorization.principalName)
        assertEquals(setOf("openid", "read"), authorization.authorizedScopes)
        assertEquals("state-token", authorization.getAttribute<String>(OAuth2ParameterNames.STATE))
        assertEquals("value", authorization.attributes["custom"])
        assertEquals("access-token", authorization.getToken(OAuth2AccessToken::class.java)!!.token.tokenValue)
        assertEquals(setOf("openid", "read"), authorization.getToken(OAuth2AccessToken::class.java)!!.token.scopes)
        assertEquals("refresh-token", authorization.getToken(OAuth2RefreshToken::class.java)!!.token.tokenValue)
        assertEquals("id-token", authorization.getToken(OidcIdToken::class.java)!!.token.tokenValue)
        assertEquals(
            "alice",
            authorization.getToken(OidcIdToken::class.java)!!.token.claims["sub"],
        )
        assertEquals("user-code", authorization.getToken(OAuth2UserCode::class.java)!!.token.tokenValue)
        assertEquals("device-code", authorization.getToken(OAuth2DeviceCode::class.java)!!.token.tokenValue)
    }

    @Test
    fun findByTokenRoutesToExpectedRepositoryMethods() {
        `when`(
            authorizationRepository.findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValueOrOidcIdTokenValueOrUserCodeValueOrDeviceCodeValue(
                "token",
            ),
        ).thenReturn(null)
        service.findByToken("token", null)
        verify(authorizationRepository)
            .findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValueOrOidcIdTokenValueOrUserCodeValueOrDeviceCodeValue("token")

        `when`(authorizationRepository.findByState("token")).thenReturn(null)
        service.findByToken("token", OAuth2TokenType(OAuth2ParameterNames.STATE))
        verify(authorizationRepository).findByState("token")

        `when`(authorizationRepository.findByAuthorizationCodeValue("token")).thenReturn(null)
        service.findByToken("token", OAuth2TokenType(OAuth2ParameterNames.CODE))
        verify(authorizationRepository).findByAuthorizationCodeValue("token")

        `when`(authorizationRepository.findByAccessTokenValue("token")).thenReturn(null)
        service.findByToken("token", OAuth2TokenType.ACCESS_TOKEN)
        verify(authorizationRepository).findByAccessTokenValue("token")

        `when`(authorizationRepository.findByRefreshTokenValue("token")).thenReturn(null)
        service.findByToken("token", OAuth2TokenType(OAuth2ParameterNames.REFRESH_TOKEN))
        verify(authorizationRepository).findByRefreshTokenValue("token")

        `when`(authorizationRepository.findByOidcIdTokenValue("token")).thenReturn(null)
        service.findByToken("token", OAuth2TokenType(OidcParameterNames.ID_TOKEN))
        verify(authorizationRepository).findByOidcIdTokenValue("token")

        `when`(authorizationRepository.findByUserCodeValue("token")).thenReturn(null)
        service.findByToken("token", OAuth2TokenType(OAuth2ParameterNames.USER_CODE))
        verify(authorizationRepository).findByUserCodeValue("token")

        `when`(authorizationRepository.findByDeviceCodeValue("token")).thenReturn(null)
        service.findByToken("token", OAuth2TokenType(OAuth2ParameterNames.DEVICE_CODE))
        verify(authorizationRepository).findByDeviceCodeValue("token")
    }

    @Test
    fun findByTokenRejectsBlankToken() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                service.findByToken(" ", OAuth2TokenType.ACCESS_TOKEN)
            }

        assertEquals("token cannot be empty", exception.message)
    }

    @Test
    fun findByTokenReturnsNullForUnsupportedTokenTypeWithoutRepositoryLookup() {
        val result = service.findByToken("token", OAuth2TokenType("unsupported"))

        assertNull(result)
        verifyNoInteractions(authorizationRepository)
    }

    @Test
    fun saveOmitsOptionalValuesWhenAuthorizationOnlyContainsRequiredFields() {
        var persisted: io.github.lishangbu.avalon.authorization.entity.OauthAuthorization? = null
        Mockito
            .doAnswer {
                persisted = it.getArgument(0)
                persisted
            }.`when`(authorizationRepository)
            .save(any())

        val minimalAuthorization =
            org.springframework.security.oauth2.server.authorization.OAuth2Authorization
                .withRegisteredClient(registeredClient())
                .id("minimal-id")
                .principalName("misty")
                .authorizationGrantType(AuthorizationGrantType("urn:custom:grant"))
                .build()

        service.save(minimalAuthorization)

        val entity = requireNotNull(persisted)
        assertEquals("minimal-id", entity.id)
        assertNull(entity.attributes)
        assertNull(entity.authorizationCodeMetadata)
        assertNull(entity.accessTokenValue)
        assertNull(entity.accessTokenMetadata)
        assertNull(entity.accessTokenScopes)
        assertNull(entity.accessTokenType)
        assertNull(entity.refreshTokenMetadata)
        assertNull(entity.oidcIdTokenMetadata)
        assertNull(entity.userCodeMetadata)
        assertNull(entity.deviceCodeMetadata)
    }

    @Test
    fun findByIdUsesFallbackClaimsAndCustomGrantTypeWhenOidcMetadataDoesNotContainClaimsEntry() {
        var entity: io.github.lishangbu.avalon.authorization.entity.OauthAuthorization? = null
        Mockito
            .doAnswer {
                entity = it.getArgument(0)
                entity
            }.`when`(authorizationRepository)
            .save(any())
        val issuedAt = Instant.parse("2026-03-25T00:00:00Z")
        val expiresAt = issuedAt.plusSeconds(300)
        val authorization =
            org.springframework.security.oauth2.server.authorization.OAuth2Authorization
                .withRegisteredClient(registeredClient())
                .id("authorization-id")
                .principalName("alice")
                .authorizationGrantType(AuthorizationGrantType("urn:custom:grant"))
                .token(OidcIdToken("id-token", issuedAt, expiresAt, mapOf("sub" to "alice"))) { metadata ->
                    metadata["nonce"] = "n-1"
                }.build()
        service.save(authorization)

        `when`(authorizationRepository.findById("authorization-id")).thenReturn(requireNotNull(entity))
        `when`(registeredClientRepository.findById("client-id")).thenReturn(registeredClient())

        val reloaded = requireNotNull(service.findById("authorization-id"))

        assertTrue(reloaded.attributes.isEmpty())
        assertEquals("urn:custom:grant", reloaded.authorizationGrantType.value)
        assertEquals("n-1", reloaded.getToken(OidcIdToken::class.java)!!.token.claims["nonce"])
    }
}
