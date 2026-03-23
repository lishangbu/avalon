package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.OauthAuthorizationConsent
import io.github.lishangbu.avalon.authorization.repository.OauthAuthorizationConsentRepository
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.stereotype.Service

/**
 * OAuth2 授权同意服务实现
 *
 * 负责 OAuth2AuthorizationConsent 的持久化、查询与删除
 *
 * @author lishangbu
 * @since 2025/8/17
 */
@Service
class DefaultOAuth2AuthorizationConsentService(
    /** OAuth 授权同意仓储 */
    private val oauth2AuthorizationConsentMapper: OauthAuthorizationConsentRepository,
    /** 注册客户端仓储 */
    private val registeredClientRepository: RegisteredClientRepository,
) : OAuth2AuthorizationConsentService {
    /** 保存 OAuth2 授权同意 */
    override fun save(authorizationConsent: OAuth2AuthorizationConsent) {
        oauth2AuthorizationConsentMapper.save(toEntity(authorizationConsent))
    }

    /** 删除 OAuth2 授权同意 */
    override fun remove(authorizationConsent: OAuth2AuthorizationConsent) {
        oauth2AuthorizationConsentMapper.deleteByRegisteredClientIdAndPrincipalName(
            authorizationConsent.registeredClientId,
            authorizationConsent.principalName,
        )
    }

    /** 按 ID 查询默认 OAuth2 授权同意 */
    override fun findById(
        registeredClientId: String,
        principalName: String,
    ): OAuth2AuthorizationConsent? {
        require(registeredClientId.isNotBlank()) { "registeredClientId cannot be empty" }
        require(principalName.isNotBlank()) { "principalName cannot be empty" }
        return oauth2AuthorizationConsentMapper
            .findByRegisteredClientIdAndPrincipalName(registeredClientId, principalName)
            ?.let(::toObject)
    }

    /** 返回转换为对象 */
    private fun toObject(oauthAuthorizationConsent: OauthAuthorizationConsent): OAuth2AuthorizationConsent {
        val registeredClientId =
            requireNotNull(oauthAuthorizationConsent.id.registeredClientId) {
                "registeredClientId cannot be null"
            }
        val principalName =
            requireNotNull(oauthAuthorizationConsent.id.principalName) {
                "principalName cannot be null"
            }
        val registeredClient: RegisteredClient? =
            registeredClientRepository.findById(registeredClientId)
        if (registeredClient == null) {
            throw DataRetrievalFailureException(
                "The RegisteredClient with id '" +
                    registeredClientId +
                    "' was not found in the OauthRegisteredClientRepository.",
            )
        }

        val builder = OAuth2AuthorizationConsent.withId(registeredClientId, principalName)
        if (oauthAuthorizationConsent.authorities != null) {
            for (authority in oauthAuthorizationConsent.authorities.toCommaDelimitedSet()) {
                builder.authority(SimpleGrantedAuthority(authority))
            }
        }

        return builder.build()
    }

    /** 返回转换为实体 */
    private fun toEntity(authorizationConsent: OAuth2AuthorizationConsent): OauthAuthorizationConsent {
        val authorities: MutableSet<String> = linkedSetOf()
        for (authority: GrantedAuthority in authorizationConsent.authorities) {
            val authorityValue = authority.authority
            if (authorityValue != null) {
                authorities.add(authorityValue)
            }
        }
        return OauthAuthorizationConsent {
            id {
                registeredClientId = authorizationConsent.registeredClientId
                principalName = authorizationConsent.principalName
            }
            this.authorities = authorities.joinToCommaDelimitedString()
        }
    }
}
