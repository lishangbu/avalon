package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.*
import io.github.lishangbu.avalon.authorization.repository.OauthAuthorizationConsentRepository
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import org.springframework.util.StringUtils

/**
 * OAuth2 授权同意服务实现
 *
 * 管理 OAuth2AuthorizationConsent（授权同意）实体的持久化与读取（基于数据表和 MyBatis 映射）
 *
 * @author lishangbu
 * @since 2025/8/17
 */
@Service
class DefaultOAuth2AuthorizationConsentService(
    authorizationConsentMapper: OauthAuthorizationConsentRepository,
    registeredClientRepository: RegisteredClientRepository,
) : OAuth2AuthorizationConsentService {
    private val oauth2AuthorizationConsentMapper: OauthAuthorizationConsentRepository
    private val registeredClientRepository: RegisteredClientRepository

    init {
        Assert.notNull(authorizationConsentMapper, "authorizationConsentMapper cannot be null")
        Assert.notNull(registeredClientRepository, "registeredClientRepository cannot be null")
        this.oauth2AuthorizationConsentMapper = authorizationConsentMapper
        this.registeredClientRepository = registeredClientRepository
    }

    override fun save(authorizationConsent: OAuth2AuthorizationConsent) {
        Assert.notNull(authorizationConsent, "authorizationConsent cannot be null")
        oauth2AuthorizationConsentMapper.save(toEntity(authorizationConsent))
    }

    override fun remove(authorizationConsent: OAuth2AuthorizationConsent) {
        Assert.notNull(authorizationConsent, "authorizationConsent cannot be null")
        oauth2AuthorizationConsentMapper.deleteByRegisteredClientIdAndPrincipalName(
            authorizationConsent.registeredClientId,
            authorizationConsent.principalName,
        )
    }

    override fun findById(
        registeredClientId: String,
        principalName: String,
    ): OAuth2AuthorizationConsent? {
        Assert.hasText(registeredClientId, "registeredClientId cannot be empty")
        Assert.hasText(principalName, "principalName cannot be empty")
        return oauth2AuthorizationConsentMapper
            .findByRegisteredClientIdAndPrincipalName(registeredClientId, principalName)
            .map(::toObject)
            .orElse(null)
    }

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
            for (
            authority in
            StringUtils.commaDelimitedListToSet(oauthAuthorizationConsent.authorities)
            ) {
                builder.authority(SimpleGrantedAuthority(authority))
            }
        }

        return builder.build()
    }

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
            this.authorities = StringUtils.collectionToCommaDelimitedString(authorities)
        }
    }
}
