package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import io.github.lishangbu.avalon.authorization.entity.dto.OauthRegisteredClientSpecification
import io.github.lishangbu.avalon.authorization.repository.Oauth2RegisteredClientRepository
import io.github.lishangbu.avalon.authorization.service.OauthRegisteredClientService
import io.github.lishangbu.avalon.jimmer.support.readOrNull
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * OAuth2 注册客户端服务实现
 *
 * 负责 OAuth2 注册客户端的查询与维护
 *
 * @author lishangbu
 * @since 2026/3/19
 */
@Service
class OauthRegisteredClientServiceImpl(
    /** OAuth2 注册客户端仓储 */
    private val oauth2RegisteredClientRepository: Oauth2RegisteredClientRepository,
) : OauthRegisteredClientService {
    /** 按条件分页查询 OAuth2 注册客户端 */
    override fun getPageByCondition(
        specification: OauthRegisteredClientSpecification,
        pageable: Pageable,
    ): Page<OauthRegisteredClient> = oauth2RegisteredClientRepository.findAll(specification, pageable)

    /** 按条件查询 OAuth2 注册客户端列表 */
    override fun listByCondition(specification: OauthRegisteredClientSpecification): List<OauthRegisteredClient> = oauth2RegisteredClientRepository.findAll(specification)

    /** 按 ID 查询 OAuth2 注册客户端 */
    override fun getById(id: String): OauthRegisteredClient? = oauth2RegisteredClientRepository.findNullable(id)

    /** 保存 OAuth2 注册客户端 */
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

    /** 更新 OAuth2 注册客户端 */
    @Transactional(rollbackFor = [Exception::class])
    override fun update(registeredClient: OauthRegisteredClient): OauthRegisteredClient = oauth2RegisteredClientRepository.save(registeredClient)

    /** 按 ID 删除 OAuth2 注册客户端 */
    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: String) {
        oauth2RegisteredClientRepository.removeById(id)
    }
}
