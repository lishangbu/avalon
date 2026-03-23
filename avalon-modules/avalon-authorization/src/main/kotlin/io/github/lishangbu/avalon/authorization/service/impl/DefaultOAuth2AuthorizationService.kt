package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.*
import io.github.lishangbu.avalon.authorization.repository.Oauth2AuthorizationRepository
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.security.jackson.SecurityJacksonModules
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

/**
 * 默认 Oauth2 授权服务实现
 *
 * 提供持久化与读取 OAuth2Authorization 的默认实现，基于数据库映射实体 `OauthAuthorization`
 *
 * @author lishangbu
 * @since 2025/11/30
 */
@Service
class DefaultOAuth2AuthorizationService(
    private val oauth2AuthorizationRepository: Oauth2AuthorizationRepository,
    private val registeredClientRepository: RegisteredClientRepository,
) : OAuth2AuthorizationService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(authorization: OAuth2Authorization) {
        val entity = toEntity(authorization)
        oauth2AuthorizationRepository.save(entity)
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun remove(authorization: OAuth2Authorization) {
        oauth2AuthorizationRepository.deleteById(authorization.id)
    }

    override fun findById(id: String): OAuth2Authorization? {
        require(id.isNotBlank()) { "id cannot be empty" }
        return oauth2AuthorizationRepository.findById(id)?.let(::toObject)
    }

    override fun findByToken(
        token: String,
        tokenType: OAuth2TokenType?,
    ): OAuth2Authorization? {
        require(token.isNotBlank()) { "token cannot be empty" }

        val result =
            when (tokenType?.value) {
                null ->
                    oauth2AuthorizationRepository
                        .findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValueOrOidcIdTokenValueOrUserCodeValueOrDeviceCodeValue(
                            token,
                        )
                OAuth2ParameterNames.STATE -> oauth2AuthorizationRepository.findByState(token)
                OAuth2ParameterNames.CODE ->
                    oauth2AuthorizationRepository.findByAuthorizationCodeValue(token)
                OAuth2ParameterNames.ACCESS_TOKEN ->
                    oauth2AuthorizationRepository.findByAccessTokenValue(token)
                OAuth2ParameterNames.REFRESH_TOKEN ->
                    oauth2AuthorizationRepository.findByRefreshTokenValue(token)
                OidcParameterNames.ID_TOKEN ->
                    oauth2AuthorizationRepository.findByOidcIdTokenValue(token)
                OAuth2ParameterNames.USER_CODE ->
                    oauth2AuthorizationRepository.findByUserCodeValue(token)
                OAuth2ParameterNames.DEVICE_CODE ->
                    oauth2AuthorizationRepository.findByDeviceCodeValue(token)
                else -> null
            }

        return result?.let(::toObject)
    }

    private fun toObject(entity: OauthAuthorization): OAuth2Authorization {
        val registeredClientId =
            requireNotNull(entity.registeredClientId) { "registeredClientId cannot be null" }
        val authorizationId = requireNotNull(entity.id) { "id cannot be null" }
        val principalName = requireNotNull(entity.principalName) { "principalName cannot be null" }
        val authorizationGrantType =
            requireNotNull(entity.authorizationGrantType) {
                "authorizationGrantType cannot be null"
            }

        val registeredClient: RegisteredClient? =
            registeredClientRepository.findById(registeredClientId)
        if (registeredClient == null) {
            throw DataRetrievalFailureException(
                "The RegisteredClient with id '" +
                    registeredClientId +
                    "' was not found in the Oauth2RegisteredClientRepository.",
            )
        }

        val builder =
            OAuth2Authorization
                .withRegisteredClient(registeredClient)
                .id(authorizationId)
                .principalName(principalName)
                .authorizationGrantType(resolveAuthorizationGrantType(authorizationGrantType))
                .authorizedScopes(entity.authorizedScopes.toCommaDelimitedSet())

        val attributes = readAttributes(entity.attributes)
        if (attributes != null) {
            builder.attributes { attributesHolder -> attributesHolder.putAll(attributes) }
        }
        if (entity.state != null) {
            builder.attribute(OAuth2ParameterNames.STATE, entity.state)
        }

        if (entity.authorizationCodeValue != null) {
            val authorizationCode =
                OAuth2AuthorizationCode(
                    entity.authorizationCodeValue,
                    entity.authorizationCodeIssuedAt,
                    entity.authorizationCodeExpiresAt,
                )
            val authorizationCodeMetadata = readAttributes(entity.authorizationCodeMetadata)
            builder.token(authorizationCode) { metadata ->
                if (authorizationCodeMetadata != null) {
                    metadata.putAll(authorizationCodeMetadata)
                }
            }
        }

        if (entity.accessTokenValue != null) {
            val accessToken =
                OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    entity.accessTokenValue,
                    entity.accessTokenIssuedAt,
                    entity.accessTokenExpiresAt,
                    entity.accessTokenScopes.toCommaDelimitedSet(),
                )
            val accessTokenMetadata = readAttributes(entity.accessTokenMetadata)
            builder.token(accessToken) { metadata ->
                if (accessTokenMetadata != null) {
                    metadata.putAll(accessTokenMetadata)
                }
            }
        }

        if (entity.refreshTokenValue != null) {
            val refreshToken =
                OAuth2RefreshToken(
                    entity.refreshTokenValue,
                    entity.refreshTokenIssuedAt,
                    entity.refreshTokenExpiresAt,
                )
            val refreshTokenMetadata = readAttributes(entity.refreshTokenMetadata)
            builder.token(refreshToken) { metadata ->
                if (refreshTokenMetadata != null) {
                    metadata.putAll(refreshTokenMetadata)
                }
            }
        }

        if (entity.oidcIdTokenValue != null) {
            val idToken =
                OidcIdToken(
                    entity.oidcIdTokenValue,
                    entity.oidcIdTokenIssuedAt,
                    entity.oidcIdTokenExpiresAt,
                    readAttributes(entity.oidcIdTokenMetadata) ?: emptyMap(),
                )
            builder.token(idToken)
        }

        if (entity.userCodeValue != null) {
            val userCode =
                OAuth2UserCode(
                    entity.userCodeValue,
                    entity.userCodeIssuedAt,
                    entity.userCodeExpiresAt,
                )
            val userCodeMetadata = readAttributes(entity.userCodeMetadata)
            builder.token(userCode) { metadata ->
                if (userCodeMetadata != null) {
                    metadata.putAll(userCodeMetadata)
                }
            }
        }

        if (entity.deviceCodeValue != null) {
            val deviceCode =
                OAuth2DeviceCode(
                    entity.deviceCodeValue,
                    entity.deviceCodeIssuedAt,
                    entity.deviceCodeExpiresAt,
                )
            val deviceCodeMetadata = readAttributes(entity.deviceCodeMetadata)
            builder.token(deviceCode) { metadata ->
                if (deviceCodeMetadata != null) {
                    metadata.putAll(deviceCodeMetadata)
                }
            }
        }

        return builder.build()
    }

    private fun toEntity(authorization: OAuth2Authorization): OauthAuthorization {
        val authorizationCodeSnapshot =
            toTokenSnapshot(authorization.getToken(OAuth2AuthorizationCode::class.java))
        val accessToken =
            authorization.getToken(OAuth2AccessToken::class.java)
        val accessTokenSnapshot = toTokenSnapshot(accessToken)
        val refreshTokenSnapshot =
            toTokenSnapshot(authorization.getToken(OAuth2RefreshToken::class.java))
        val oidcIdTokenSnapshot =
            toTokenSnapshot(authorization.getToken(OidcIdToken::class.java))
        val userCodeSnapshot =
            toTokenSnapshot(authorization.getToken(OAuth2UserCode::class.java))
        val deviceCodeSnapshot =
            toTokenSnapshot(authorization.getToken(OAuth2DeviceCode::class.java))

        return OauthAuthorization {
            id = authorization.id
            registeredClientId = authorization.registeredClientId
            principalName = authorization.principalName
            authorizationGrantType = authorization.authorizationGrantType.value
            authorizedScopes = authorization.authorizedScopes.joinToCommaDelimitedString()
            attributes = writeAttributes(authorization.attributes)
            state = authorization.getAttribute(OAuth2ParameterNames.STATE)

            authorizationCodeValue = authorizationCodeSnapshot.value
            authorizationCodeIssuedAt = authorizationCodeSnapshot.issuedAt
            authorizationCodeExpiresAt = authorizationCodeSnapshot.expiresAt
            authorizationCodeMetadata = writeAttributes(authorizationCodeSnapshot.metadata)

            accessTokenValue = accessTokenSnapshot.value
            accessTokenIssuedAt = accessTokenSnapshot.issuedAt
            accessTokenExpiresAt = accessTokenSnapshot.expiresAt
            accessTokenMetadata = writeAttributes(accessTokenSnapshot.metadata)
            accessTokenScopes = accessToken?.token?.scopes?.joinToCommaDelimitedString()
            accessTokenType = accessToken?.token?.tokenType?.value

            refreshTokenValue = refreshTokenSnapshot.value
            refreshTokenIssuedAt = refreshTokenSnapshot.issuedAt
            refreshTokenExpiresAt = refreshTokenSnapshot.expiresAt
            refreshTokenMetadata = writeAttributes(refreshTokenSnapshot.metadata)

            oidcIdTokenValue = oidcIdTokenSnapshot.value
            oidcIdTokenIssuedAt = oidcIdTokenSnapshot.issuedAt
            oidcIdTokenExpiresAt = oidcIdTokenSnapshot.expiresAt
            oidcIdTokenMetadata = writeAttributes(oidcIdTokenSnapshot.metadata)

            userCodeValue = userCodeSnapshot.value
            userCodeIssuedAt = userCodeSnapshot.issuedAt
            userCodeExpiresAt = userCodeSnapshot.expiresAt
            userCodeMetadata = writeAttributes(userCodeSnapshot.metadata)

            deviceCodeValue = deviceCodeSnapshot.value
            deviceCodeIssuedAt = deviceCodeSnapshot.issuedAt
            deviceCodeExpiresAt = deviceCodeSnapshot.expiresAt
            deviceCodeMetadata = writeAttributes(deviceCodeSnapshot.metadata)
        }
    }

    private fun toTokenSnapshot(token: OAuth2Authorization.Token<*>?): TokenSnapshot {
        if (token == null) {
            return TokenSnapshot()
        }
        val oAuth2Token: OAuth2Token = token.token
        return TokenSnapshot(
            value = oAuth2Token.tokenValue,
            issuedAt = oAuth2Token.issuedAt,
            expiresAt = oAuth2Token.expiresAt,
            metadata = token.metadata,
        )
    }

    private fun readAttributes(json: String?): Map<String, Any>? {
        if (json.isNullOrBlank()) {
            return null
        }
        return mapper.readValue(json, object : TypeReference<Map<String, Any>>() {})
    }

    private fun writeAttributes(attributes: Map<String, Any>?): String? {
        if (attributes.isNullOrEmpty()) {
            return null
        }
        return mapper.writeValueAsString(attributes)
    }

    private data class TokenSnapshot(
        val value: String? = null,
        val issuedAt: Instant? = null,
        val expiresAt: Instant? = null,
        val metadata: Map<String, Any>? = null,
    )

    companion object {
        private val mapper: JsonMapper =
            JsonMapper
                .builder()
                .addModules(SecurityJacksonModules.getModules(DefaultOAuth2AuthorizationService::class.java.classLoader))
                .build()

        private fun resolveAuthorizationGrantType(
            authorizationGrantType: String,
        ): AuthorizationGrantType =
            when (authorizationGrantType) {
                AuthorizationGrantType.AUTHORIZATION_CODE.value ->
                    AuthorizationGrantType.AUTHORIZATION_CODE
                AuthorizationGrantType.CLIENT_CREDENTIALS.value ->
                    AuthorizationGrantType.CLIENT_CREDENTIALS
                AuthorizationGrantType.REFRESH_TOKEN.value -> AuthorizationGrantType.REFRESH_TOKEN
                AuthorizationGrantType.DEVICE_CODE.value -> AuthorizationGrantType.DEVICE_CODE
                else -> AuthorizationGrantType(authorizationGrantType)
            }
    }
}
