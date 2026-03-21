package io.github.lishangbu.avalon.authorization.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.Instant

@Entity
@Table(name = "oauth_authorization")
interface OauthAuthorization {
    @Id
    val id: String

    val registeredClientId: String?

    val principalName: String?

    val authorizationGrantType: String?

    val authorizedScopes: String?

    val attributes: String?

    val state: String?

    val authorizationCodeValue: String?

    val authorizationCodeIssuedAt: Instant?

    val authorizationCodeExpiresAt: Instant?

    val authorizationCodeMetadata: String?

    val accessTokenValue: String?

    val accessTokenIssuedAt: Instant?

    val accessTokenExpiresAt: Instant?

    val accessTokenMetadata: String?

    val accessTokenType: String?

    val accessTokenScopes: String?

    val oidcIdTokenValue: String?

    val oidcIdTokenIssuedAt: Instant?

    val oidcIdTokenExpiresAt: Instant?

    val oidcIdTokenMetadata: String?

    val refreshTokenValue: String?

    val refreshTokenIssuedAt: Instant?

    val refreshTokenExpiresAt: Instant?

    val refreshTokenMetadata: String?

    val userCodeValue: String?

    val userCodeIssuedAt: Instant?

    val userCodeExpiresAt: Instant?

    val userCodeMetadata: String?

    val deviceCodeValue: String?

    val deviceCodeIssuedAt: Instant?

    val deviceCodeExpiresAt: Instant?

    val deviceCodeMetadata: String?
}
