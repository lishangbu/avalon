package org.springframework.security.oauth2.server.authorization.authentication

import org.springframework.security.oauth2.core.ClaimAccessor
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2Token
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext
import java.time.Duration

internal const val TOKEN_REQUEST_ERROR_URI =
    "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2"

internal fun buildLockMessage(remainingLock: Duration?): String {
    val seconds = maxOf(1L, remainingLock?.seconds ?: 0L)
    if (seconds >= 60) {
        val minutes = (seconds + 59) / 60
        return "账号已被锁定，请在${minutes}分钟后重试"
    }
    return "账号已被锁定，请在${seconds}秒后重试"
}

internal fun buildAccessToken(
    authorizationBuilder: OAuth2Authorization.Builder,
    generatedAccessToken: OAuth2Token,
    tokenContext: OAuth2TokenContext,
): OAuth2AccessToken {
    var tokenType = OAuth2AccessToken.TokenType.BEARER
    if (generatedAccessToken is ClaimAccessor) {
        val cnfClaims = generatedAccessToken.getClaimAsMap("cnf")
        if (!cnfClaims.isNullOrEmpty() && cnfClaims.containsKey("jkt")) {
            tokenType = OAuth2AccessToken.TokenType.DPOP
        }
    }

    val accessToken =
        OAuth2AccessToken(
            tokenType,
            generatedAccessToken.tokenValue,
            generatedAccessToken.issuedAt,
            generatedAccessToken.expiresAt,
            tokenContext.authorizedScopes,
        )
    val accessTokenFormat = tokenContext.registeredClient.tokenSettings.accessTokenFormat

    authorizationBuilder.token(accessToken) { metadata ->
        if (generatedAccessToken is ClaimAccessor) {
            metadata[OAuth2Authorization.Token.CLAIMS_METADATA_NAME] = generatedAccessToken.claims
        }
        metadata[OAuth2Authorization.Token.INVALIDATED_METADATA_NAME] = false
        metadata[OAuth2TokenFormat::class.java.name] = accessTokenFormat.value
    }

    return accessToken
}
