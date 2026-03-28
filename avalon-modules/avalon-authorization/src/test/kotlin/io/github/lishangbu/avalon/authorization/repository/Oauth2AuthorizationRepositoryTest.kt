package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.OauthAuthorization
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Transactional
class Oauth2AuthorizationRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var oauth2AuthorizationRepository: Oauth2AuthorizationRepository

    @Test
    fun shouldSaveFindAndDeleteAuthorizationByEachToken() {
        val issuedAt = Instant.parse("2026-03-25T00:00:00Z")
        val expiresAt = issuedAt.plusSeconds(300)
        val authorization =
            OauthAuthorization {
                id = "unit-authorization"
                registeredClientId = "1"
                principalName = "alice"
                authorizationGrantType = "password"
                authorizedScopes = "read,openid"
                attributes = """{"custom":"value"}"""
                state = "state-token"
                authorizationCodeValue = "code-token"
                authorizationCodeIssuedAt = issuedAt
                authorizationCodeExpiresAt = expiresAt
                authorizationCodeMetadata = """{"code":true}"""
                accessTokenValue = "access-token"
                accessTokenIssuedAt = issuedAt
                accessTokenExpiresAt = expiresAt
                accessTokenMetadata = """{"access":true}"""
                accessTokenType = "Bearer"
                accessTokenScopes = "read,openid"
                refreshTokenValue = "refresh-token"
                refreshTokenIssuedAt = issuedAt
                refreshTokenExpiresAt = expiresAt.plusSeconds(300)
                refreshTokenMetadata = """{"refresh":true}"""
                oidcIdTokenValue = "id-token"
                oidcIdTokenIssuedAt = issuedAt
                oidcIdTokenExpiresAt = expiresAt
                oidcIdTokenMetadata = """{"claims":{"sub":"alice"}}"""
                userCodeValue = "user-code"
                userCodeIssuedAt = issuedAt
                userCodeExpiresAt = expiresAt
                userCodeMetadata = """{"user":true}"""
                deviceCodeValue = "device-code"
                deviceCodeIssuedAt = issuedAt
                deviceCodeExpiresAt = expiresAt
                deviceCodeMetadata = """{"device":true}"""
            }

        oauth2AuthorizationRepository.save(authorization)
        oauth2AuthorizationRepository.save(OauthAuthorization(authorization) { principalName = "alice-updated" })

        assertEquals("alice-updated", requireNotNull(oauth2AuthorizationRepository.findNullable("unit-authorization")).principalName)
        assertEquals("unit-authorization", oauth2AuthorizationRepository.findByState("state-token")!!.id)
        assertEquals("unit-authorization", oauth2AuthorizationRepository.findByAuthorizationCodeValue("code-token")!!.id)
        assertEquals("unit-authorization", oauth2AuthorizationRepository.findByAccessTokenValue("access-token")!!.id)
        assertEquals("unit-authorization", oauth2AuthorizationRepository.findByRefreshTokenValue("refresh-token")!!.id)
        assertEquals("unit-authorization", oauth2AuthorizationRepository.findByOidcIdTokenValue("id-token")!!.id)
        assertEquals("unit-authorization", oauth2AuthorizationRepository.findByUserCodeValue("user-code")!!.id)
        assertEquals("unit-authorization", oauth2AuthorizationRepository.findByDeviceCodeValue("device-code")!!.id)

        assertEquals(
            "unit-authorization",
            oauth2AuthorizationRepository
                .findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValueOrOidcIdTokenValueOrUserCodeValueOrDeviceCodeValue("refresh-token")!!
                .id,
        )
        assertEquals(
            "unit-authorization",
            oauth2AuthorizationRepository
                .findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValueOrOidcIdTokenValueOrUserCodeValueOrDeviceCodeValue("id-token")!!
                .id,
        )
        assertEquals(
            "unit-authorization",
            oauth2AuthorizationRepository
                .findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValueOrOidcIdTokenValueOrUserCodeValueOrDeviceCodeValue("user-code")!!
                .id,
        )
        assertEquals(
            "unit-authorization",
            oauth2AuthorizationRepository
                .findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValueOrOidcIdTokenValueOrUserCodeValueOrDeviceCodeValue("device-code")!!
                .id,
        )

        oauth2AuthorizationRepository.removeById("unit-authorization")
        assertNull(oauth2AuthorizationRepository.findNullable("unit-authorization"))
    }
}
