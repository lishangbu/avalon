package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.OauthAuthorizationConsent
import io.github.lishangbu.avalon.authorization.entity.OauthAuthorizationConsentId
import java.util.*

/**
 * 用户授权确认表(oauth_authorization_consent)数据库访问层
 *
 * 提供基础的 CRUD 操作
 *
 * @author lishangbu
 * @since 2025/9/14
 */
interface OauthAuthorizationConsentRepository {
    fun findById(id: OauthAuthorizationConsentId): Optional<OauthAuthorizationConsent>

    fun save(consent: OauthAuthorizationConsent): OauthAuthorizationConsent

    fun saveAndFlush(consent: OauthAuthorizationConsent): OauthAuthorizationConsent

    fun flush()

    fun findByRegisteredClientIdAndPrincipalName(
        registeredClientId: String,
        principalName: String,
    ): Optional<OauthAuthorizationConsent>

    fun deleteByRegisteredClientIdAndPrincipalName(
        registeredClientId: String,
        principalName: String,
    )
}
