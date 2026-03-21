package io.github.lishangbu.avalon.authorization.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.Instant

@Entity
@Table(name = "oauth_registered_client")
interface OauthRegisteredClient {
    @Id
    val id: String

    val clientId: String?

    val clientIdIssuedAt: Instant?

    val clientSecret: String?

    val clientSecretExpiresAt: Instant?

    val clientName: String?

    val clientAuthenticationMethods: String?

    val authorizationGrantTypes: String?

    val redirectUris: String?

    val postLogoutRedirectUris: String?

    val scopes: String?

    val requireProofKey: Boolean?

    val requireAuthorizationConsent: Boolean?

    val jwkSetUrl: String?

    val tokenEndpointAuthenticationSigningAlgorithm: String?

    val x509CertificateSubjectDn: String?

    val authorizationCodeTimeToLive: String?

    val accessTokenTimeToLive: String?

    val accessTokenFormat: String?

    val deviceCodeTimeToLive: String?

    val reuseRefreshTokens: Boolean?

    val refreshTokenTimeToLive: String?

    val idTokenSignatureAlgorithm: String?

    val x509CertificateBoundAccessTokens: Boolean?
}
