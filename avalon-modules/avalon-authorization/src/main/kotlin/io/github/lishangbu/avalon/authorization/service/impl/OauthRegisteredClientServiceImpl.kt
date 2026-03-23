package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.*
import io.github.lishangbu.avalon.authorization.repository.Oauth2RegisteredClientRepository
import io.github.lishangbu.avalon.authorization.repository.readOrNull
import io.github.lishangbu.avalon.authorization.service.OauthRegisteredClientService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * OAuth2 注册客户端服务实现
 *
 * @author lishangbu
 * @since 2026/3/19
 */
@Service
class OauthRegisteredClientServiceImpl(
    private val oauth2RegisteredClientRepository: Oauth2RegisteredClientRepository,
) : OauthRegisteredClientService {
    override fun getPageByCondition(
        registeredClient: OauthRegisteredClient,
        pageable: Pageable,
    ): Page<OauthRegisteredClient> =
        oauth2RegisteredClientRepository.findAll(
            Example.of(
                registeredClient,
                ExampleMatcher
                    .matching()
                    .withIgnoreNullValues()
                    .withMatcher("clientId", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("clientName", ExampleMatcher.GenericPropertyMatchers.contains()),
            ),
            pageable,
        )

    override fun listByCondition(registeredClient: OauthRegisteredClient): List<OauthRegisteredClient> =
        oauth2RegisteredClientRepository.findAll(
            Example.of(
                registeredClient,
                ExampleMatcher
                    .matching()
                    .withIgnoreNullValues()
                    .withMatcher("clientId", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("clientName", ExampleMatcher.GenericPropertyMatchers.contains()),
            ),
        )

    override fun getById(id: String): Optional<OauthRegisteredClient> = oauth2RegisteredClientRepository.findById(id)

    @Transactional(rollbackFor = [Exception::class])
    override fun save(registeredClient: OauthRegisteredClient): OauthRegisteredClient {
        val now = Instant.now()
        val newId = registeredClient.readOrNull { id }?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val saved =
            OauthRegisteredClient {
                id = newId
                clientId = registeredClient.readOrNull { clientId }
                clientIdIssuedAt = registeredClient.readOrNull { clientIdIssuedAt } ?: now
                clientSecret = registeredClient.readOrNull { clientSecret }
                clientSecretExpiresAt = registeredClient.readOrNull { clientSecretExpiresAt }
                clientName = registeredClient.readOrNull { clientName }
                clientAuthenticationMethods = registeredClient.readOrNull { clientAuthenticationMethods }
                authorizationGrantTypes = registeredClient.readOrNull { authorizationGrantTypes }
                redirectUris = registeredClient.readOrNull { redirectUris }
                postLogoutRedirectUris = registeredClient.readOrNull { postLogoutRedirectUris }
                scopes = registeredClient.readOrNull { scopes }
                requireProofKey = registeredClient.readOrNull { requireProofKey }
                requireAuthorizationConsent = registeredClient.readOrNull { requireAuthorizationConsent }
                jwkSetUrl = registeredClient.readOrNull { jwkSetUrl }
                tokenEndpointAuthenticationSigningAlgorithm =
                    registeredClient.readOrNull { tokenEndpointAuthenticationSigningAlgorithm }
                x509CertificateSubjectDn = registeredClient.readOrNull { x509CertificateSubjectDn }
                authorizationCodeTimeToLive = registeredClient.readOrNull { authorizationCodeTimeToLive }
                accessTokenTimeToLive = registeredClient.readOrNull { accessTokenTimeToLive }
                accessTokenFormat = registeredClient.readOrNull { accessTokenFormat }
                deviceCodeTimeToLive = registeredClient.readOrNull { deviceCodeTimeToLive }
                reuseRefreshTokens = registeredClient.readOrNull { reuseRefreshTokens }
                refreshTokenTimeToLive = registeredClient.readOrNull { refreshTokenTimeToLive }
                idTokenSignatureAlgorithm = registeredClient.readOrNull { idTokenSignatureAlgorithm }
                x509CertificateBoundAccessTokens =
                    registeredClient.readOrNull { x509CertificateBoundAccessTokens }
            }
        return oauth2RegisteredClientRepository.save(saved)
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun update(registeredClient: OauthRegisteredClient): OauthRegisteredClient = oauth2RegisteredClientRepository.save(registeredClient)

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: String) {
        oauth2RegisteredClientRepository.deleteById(id)
    }
}
