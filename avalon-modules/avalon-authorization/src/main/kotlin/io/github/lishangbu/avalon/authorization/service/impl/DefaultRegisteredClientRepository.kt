package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import io.github.lishangbu.avalon.authorization.repository.Oauth2RegisteredClientRepository
import org.springframework.boot.convert.DurationStyle
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.stereotype.Component
import java.time.temporal.ChronoUnit

/**
 * RegisteredClient 仓储适配器
 *
 * 负责在 Spring Authorization Server 的 [RegisteredClient] 与持久化实体之间转换
 *
 * @author lishangbu
 * @since 2025/8/17
 */
@Component
class DefaultRegisteredClientRepository(
    /** OAuth2 注册客户端仓储 */
    private val oauth2RegisteredClientRepository: Oauth2RegisteredClientRepository,
) : RegisteredClientRepository {
    /**
     * 保存注册客户端
     *
     * @param registeredClient 注册客户端
     */
    override fun save(registeredClient: RegisteredClient) {
        oauth2RegisteredClientRepository.save(toEntity(registeredClient))
    }

    /**
     * 按 ID 查询注册客户端
     *
     * @param id 客户端 ID
     * @return 注册客户端，未找到时返回 null
     */
    override fun findById(id: String): RegisteredClient? {
        require(id.isNotBlank()) { "id cannot be empty" }
        return oauth2RegisteredClientRepository.findById(id)?.let(::toObject)
    }

    /**
     * 按客户端 ID 查询注册客户端
     *
     * @param clientId 客户端 ID
     * @return 注册客户端，未找到时返回 null
     */
    override fun findByClientId(clientId: String): RegisteredClient? {
        require(clientId.isNotBlank()) { "clientId cannot be empty" }
        return oauth2RegisteredClientRepository
            .findByClientId(clientId)
            ?.let(::toObject)
    }

    /**
     * 将实体转换为对象
     *
     * @param client 实体
     * @return 对象
     */
    private fun toObject(client: OauthRegisteredClient): RegisteredClient {
        val clientAuthenticationMethods = client.clientAuthenticationMethods.toCommaDelimitedSet()
        val authorizationGrantTypes = client.authorizationGrantTypes.toCommaDelimitedSet()
        val redirectUris = client.redirectUris.toCommaDelimitedSet()
        val postLogoutRedirectUris = client.postLogoutRedirectUris.toCommaDelimitedSet()
        val clientScopes = client.scopes.toCommaDelimitedSet()

        val builder =
            RegisteredClient
                .withId(client.id)
                .clientId(client.clientId)
                .clientIdIssuedAt(client.clientIdIssuedAt)
                .clientSecret(client.clientSecret)
                .clientSecretExpiresAt(client.clientSecretExpiresAt)
                .clientName(client.clientName)
                .clientAuthenticationMethods { authenticationMethods ->
                    clientAuthenticationMethods.forEach { authenticationMethod ->
                        authenticationMethods.add(
                            resolveClientAuthenticationMethod(authenticationMethod),
                        )
                    }
                }.authorizationGrantTypes { grantTypes ->
                    authorizationGrantTypes.forEach { grantType ->
                        grantTypes.add(resolveAuthorizationGrantType(grantType))
                    }
                }.redirectUris { uris -> uris.addAll(redirectUris) }
                .postLogoutRedirectUris { uris -> uris.addAll(postLogoutRedirectUris) }
                .scopes { scopes -> scopes.addAll(clientScopes) }

        val clientSettingsBuilder = ClientSettings.builder()
        client.requireProofKey?.let { clientSettingsBuilder.requireProofKey(it) }
        client.requireAuthorizationConsent?.let {
            clientSettingsBuilder.requireAuthorizationConsent(it)
        }
        client.jwkSetUrl?.let { clientSettingsBuilder.jwkSetUrl(it) }
        if (client.tokenEndpointAuthenticationSigningAlgorithm != null) {
            val signatureAlgorithm =
                SignatureAlgorithm.from(client.tokenEndpointAuthenticationSigningAlgorithm)
            if (signatureAlgorithm != null) {
                clientSettingsBuilder.tokenEndpointAuthenticationSigningAlgorithm(
                    signatureAlgorithm,
                )
            } else {
                val macAlgorithm =
                    MacAlgorithm.from(client.tokenEndpointAuthenticationSigningAlgorithm)
                if (macAlgorithm != null) {
                    clientSettingsBuilder.tokenEndpointAuthenticationSigningAlgorithm(macAlgorithm)
                }
            }
        }
        client.x509CertificateSubjectDn?.let { clientSettingsBuilder.x509CertificateSubjectDN(it) }
        builder.clientSettings(clientSettingsBuilder.build())

        val tokenSettingsBuilder = TokenSettings.builder()
        client.reuseRefreshTokens?.let { tokenSettingsBuilder.reuseRefreshTokens(it) }
        client.x509CertificateBoundAccessTokens?.let {
            tokenSettingsBuilder.x509CertificateBoundAccessTokens(it)
        }
        client.authorizationCodeTimeToLive?.let {
            tokenSettingsBuilder.authorizationCodeTimeToLive(
                DurationStyle.detectAndParse(it, ChronoUnit.SECONDS),
            )
        }
        client.accessTokenTimeToLive?.let {
            tokenSettingsBuilder.accessTokenTimeToLive(
                DurationStyle.detectAndParse(it, ChronoUnit.SECONDS),
            )
        }
        client.accessTokenFormat?.let {
            tokenSettingsBuilder.accessTokenFormat(OAuth2TokenFormat(it))
        }
        client.deviceCodeTimeToLive?.let {
            tokenSettingsBuilder.deviceCodeTimeToLive(
                DurationStyle.detectAndParse(it, ChronoUnit.SECONDS),
            )
        }
        client.refreshTokenTimeToLive?.let {
            tokenSettingsBuilder.refreshTokenTimeToLive(
                DurationStyle.detectAndParse(it, ChronoUnit.SECONDS),
            )
        }
        client.idTokenSignatureAlgorithm?.let {
            SignatureAlgorithm.from(it)?.let { alg ->
                tokenSettingsBuilder.idTokenSignatureAlgorithm(alg)
            }
        }
        builder.tokenSettings(tokenSettingsBuilder.build())

        return builder.build()
    }

    /**
     * 将对象转换为实体
     *
     * @param registeredClient 对象
     * @return 实体
     */
    private fun toEntity(registeredClient: RegisteredClient): OauthRegisteredClient {
        val clientAuthenticationMethodValues =
            registeredClient.clientAuthenticationMethods.map { it.value }

        val authorizationGrantTypeValues = registeredClient.authorizationGrantTypes.map { it.value }
        val registeredClientSettings = registeredClient.clientSettings
        val registeredClientTokenSettings = registeredClient.tokenSettings

        return OauthRegisteredClient {
            id = registeredClient.id
            clientId = registeredClient.clientId
            clientIdIssuedAt = registeredClient.clientIdIssuedAt
            clientSecret = registeredClient.clientSecret
            clientSecretExpiresAt = registeredClient.clientSecretExpiresAt
            clientName = registeredClient.clientName
            this.clientAuthenticationMethods =
                clientAuthenticationMethodValues.joinToCommaDelimitedString()
            this.authorizationGrantTypes = authorizationGrantTypeValues.joinToCommaDelimitedString()
            redirectUris = registeredClient.redirectUris.joinToCommaDelimitedString()
            postLogoutRedirectUris =
                registeredClient.postLogoutRedirectUris.joinToCommaDelimitedString()
            scopes = registeredClient.scopes.joinToCommaDelimitedString()

            requireProofKey = registeredClientSettings?.isRequireProofKey
            requireAuthorizationConsent =
                registeredClientSettings?.isRequireAuthorizationConsent
            tokenEndpointAuthenticationSigningAlgorithm =
                registeredClientSettings?.tokenEndpointAuthenticationSigningAlgorithm?.name
            jwkSetUrl = registeredClientSettings?.jwkSetUrl
            x509CertificateSubjectDn = registeredClientSettings?.x509CertificateSubjectDN

            reuseRefreshTokens = registeredClientTokenSettings?.isReuseRefreshTokens
            x509CertificateBoundAccessTokens =
                registeredClientTokenSettings?.isX509CertificateBoundAccessTokens
            authorizationCodeTimeToLive =
                registeredClientTokenSettings?.authorizationCodeTimeToLive?.toString()
            accessTokenTimeToLive =
                registeredClientTokenSettings?.accessTokenTimeToLive?.toString()
            accessTokenFormat = registeredClientTokenSettings?.accessTokenFormat?.value
            deviceCodeTimeToLive = registeredClientTokenSettings?.deviceCodeTimeToLive?.toString()
            refreshTokenTimeToLive =
                registeredClientTokenSettings?.refreshTokenTimeToLive?.toString()
            idTokenSignatureAlgorithm =
                registeredClientTokenSettings?.idTokenSignatureAlgorithm?.name
        }
    }

    companion object {
        /**
         * 解析授权类型
         *
         * @param authorizationGrantType 授权类型字符串
         * @return 授权类型对象
         */
        private fun resolveAuthorizationGrantType(
            authorizationGrantType: String,
        ): AuthorizationGrantType =
            when (authorizationGrantType) {
                AuthorizationGrantType.AUTHORIZATION_CODE.value ->
                    AuthorizationGrantType.AUTHORIZATION_CODE

                AuthorizationGrantType.CLIENT_CREDENTIALS.value ->
                    AuthorizationGrantType.CLIENT_CREDENTIALS

                AuthorizationGrantType.REFRESH_TOKEN.value -> AuthorizationGrantType.REFRESH_TOKEN
                else -> AuthorizationGrantType(authorizationGrantType)
            }

        /**
         * 解析客户端认证方法
         *
         * @param clientAuthenticationMethod 认证方法字符串
         * @return 认证方法对象
         */
        private fun resolveClientAuthenticationMethod(
            clientAuthenticationMethod: String,
        ): ClientAuthenticationMethod =
            when (clientAuthenticationMethod) {
                ClientAuthenticationMethod.CLIENT_SECRET_BASIC.value ->
                    ClientAuthenticationMethod.CLIENT_SECRET_BASIC

                ClientAuthenticationMethod.CLIENT_SECRET_POST.value ->
                    ClientAuthenticationMethod.CLIENT_SECRET_POST

                ClientAuthenticationMethod.NONE.value -> ClientAuthenticationMethod.NONE
                else -> ClientAuthenticationMethod(clientAuthenticationMethod)
            }
    }
}
