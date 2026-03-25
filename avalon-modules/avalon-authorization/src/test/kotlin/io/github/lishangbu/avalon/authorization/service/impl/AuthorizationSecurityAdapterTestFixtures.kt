package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.OauthAuthorization
import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import io.github.lishangbu.avalon.authorization.repository.Oauth2AuthorizationRepository
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2DeviceCode
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.OAuth2UserCode
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import java.time.Duration
import java.time.Instant

internal fun oauth2Authorization(): OAuth2Authorization {
    val issuedAt = Instant.parse("2026-03-25T00:00:00Z")
    val expiresAt = issuedAt.plusSeconds(300)
    val accessToken =
        OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "access-token",
            issuedAt,
            expiresAt,
            setOf("openid", "read"),
        )
    val refreshToken = OAuth2RefreshToken("refresh-token", issuedAt, expiresAt.plusSeconds(300))
    val idToken = OidcIdToken("id-token", issuedAt, expiresAt, mapOf("sub" to "alice"))
    val userCode = OAuth2UserCode("user-code", issuedAt, expiresAt)
    val deviceCode = OAuth2DeviceCode("device-code", issuedAt, expiresAt)
    return OAuth2Authorization
        .withRegisteredClient(registeredClient())
        .id("authorization-id")
        .principalName("alice")
        .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
        .authorizedScopes(setOf("openid", "read"))
        .attribute("custom", "value")
        .attribute(OAuth2ParameterNames.STATE, "state-token")
        .token(OAuth2AuthorizationCode("auth-code", issuedAt, expiresAt)) { metadata ->
            metadata["code"] = true
        }.token(accessToken) { metadata ->
            metadata["access"] = true
        }.refreshToken(refreshToken)
        .token(idToken) { metadata ->
            metadata[OAuth2Authorization.Token.CLAIMS_METADATA_NAME] = idToken.claims
        }.token(userCode) { metadata ->
            metadata["user"] = true
        }.token(deviceCode) { metadata ->
            metadata["device"] = true
        }.build()
}

internal fun authorizationEntity(): OauthAuthorization {
    val issuedAt = Instant.parse("2026-03-25T00:00:00Z")
    val expiresAt = issuedAt.plusSeconds(300)
    return OauthAuthorization {
        id = "authorization-id"
        registeredClientId = "client-id"
        principalName = "alice"
        authorizationGrantType = AuthorizationGrantTypeSupport.PASSWORD.value
        authorizedScopes = "openid,read"
        attributes = """{"custom":"value"}"""
        state = "state-token"
        authorizationCodeValue = "auth-code"
        authorizationCodeIssuedAt = issuedAt
        authorizationCodeExpiresAt = expiresAt
        authorizationCodeMetadata = """{"code":true}"""
        accessTokenValue = "access-token"
        accessTokenIssuedAt = issuedAt
        accessTokenExpiresAt = expiresAt
        accessTokenMetadata = """{"access":true}"""
        accessTokenType = "Bearer"
        accessTokenScopes = "openid,read"
        refreshTokenValue = "refresh-token"
        refreshTokenIssuedAt = issuedAt
        refreshTokenExpiresAt = expiresAt.plusSeconds(300)
        refreshTokenMetadata = """{"refresh":true}"""
        oidcIdTokenValue = "id-token"
        oidcIdTokenIssuedAt = issuedAt
        oidcIdTokenExpiresAt = expiresAt
        oidcIdTokenMetadata = """{"sub":"alice"}"""
        userCodeValue = "user-code"
        userCodeIssuedAt = issuedAt
        userCodeExpiresAt = expiresAt
        userCodeMetadata = """{"user":true}"""
        deviceCodeValue = "device-code"
        deviceCodeIssuedAt = issuedAt
        deviceCodeExpiresAt = expiresAt
        deviceCodeMetadata = """{"device":true}"""
    }
}

internal fun roundTripAuthorizationEntity(): OauthAuthorization {
    val repository = mock(Oauth2AuthorizationRepository::class.java)
    val registeredClientRepository = mock(RegisteredClientRepository::class.java)
    val service = DefaultOAuth2AuthorizationService(repository, registeredClientRepository)
    var persisted: OauthAuthorization? = null
    Mockito.doAnswer {
        persisted = it.getArgument(0)
        persisted
    }.`when`(repository).save(any())
    service.save(oauth2Authorization())
    return requireNotNull(persisted)
}

internal fun registeredClient(): RegisteredClient =
    RegisteredClient
        .withId("client-id")
        .clientId("client")
        .clientIdIssuedAt(Instant.parse("2026-03-25T00:00:00Z"))
        .clientSecret("secret")
        .clientSecretExpiresAt(Instant.parse("2026-03-26T00:00:00Z"))
        .clientName("Client")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .clientAuthenticationMethod(ClientAuthenticationMethod("private_key_jwt"))
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .authorizationGrantType(AuthorizationGrantType("urn:custom:grant"))
        .redirectUri("https://app.example/callback")
        .redirectUri("https://app.example/alt")
        .postLogoutRedirectUri("https://app.example/logout")
        .scope("openid")
        .scope("profile")
        .clientSettings(
            ClientSettings
                .builder()
                .requireProofKey(true)
                .requireAuthorizationConsent(false)
                .tokenEndpointAuthenticationSigningAlgorithm(MacAlgorithm.HS256)
                .jwkSetUrl("https://issuer.example/jwks")
                .x509CertificateSubjectDN("CN=client")
                .build(),
        ).tokenSettings(
            TokenSettings
                .builder()
                .authorizationCodeTimeToLive(Duration.ofMinutes(1))
                .accessTokenTimeToLive(Duration.ofMinutes(2))
                .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                .deviceCodeTimeToLive(Duration.ofMinutes(3))
                .reuseRefreshTokens(false)
                .refreshTokenTimeToLive(Duration.ofMinutes(4))
                .idTokenSignatureAlgorithm(SignatureAlgorithm.RS256)
                .x509CertificateBoundAccessTokens(true)
                .build(),
        ).build()

internal fun registeredClientEntity(
    id: String,
    tokenEndpointAuthenticationSigningAlgorithm: String,
): OauthRegisteredClient =
    OauthRegisteredClient {
        this.id = id
        clientId = "client"
        clientIdIssuedAt = Instant.parse("2026-03-25T00:00:00Z")
        clientSecret = "secret"
        clientSecretExpiresAt = Instant.parse("2026-03-26T00:00:00Z")
        clientName = "Client"
        clientAuthenticationMethods = "client_secret_basic,private_key_jwt"
        authorizationGrantTypes = "authorization_code,refresh_token,urn:custom:grant"
        redirectUris = "https://app.example/callback,https://app.example/alt"
        postLogoutRedirectUris = "https://app.example/logout"
        scopes = "openid,profile"
        requireProofKey = true
        requireAuthorizationConsent = false
        jwkSetUrl = "https://issuer.example/jwks"
        this.tokenEndpointAuthenticationSigningAlgorithm = tokenEndpointAuthenticationSigningAlgorithm
        x509CertificateSubjectDn = "CN=client"
        authorizationCodeTimeToLive = "PT1M"
        accessTokenTimeToLive = "PT2M"
        accessTokenFormat = OAuth2TokenFormat.REFERENCE.value
        deviceCodeTimeToLive = "PT3M"
        reuseRefreshTokens = false
        refreshTokenTimeToLive = "PT4M"
        idTokenSignatureAlgorithm = SignatureAlgorithm.RS256.name
        x509CertificateBoundAccessTokens = true
    }
