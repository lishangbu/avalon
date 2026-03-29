package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.OauthAuthorizationConsent
import io.github.lishangbu.avalon.authorization.entity.OauthAuthorizationConsentId
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * OAuth 授权同意仓储接口
 *
 * 定义OAuth 授权同意数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/9/14
 */
interface OauthAuthorizationConsentRepository : KRepository<OauthAuthorizationConsent, OauthAuthorizationConsentId> {
    /** 按注册客户端 ID 和主体名称查询 OAuth 授权同意 */
    fun findByRegisteredClientIdAndPrincipalName(
        registeredClientId: String,
        principalName: String,
    ): OauthAuthorizationConsent? =
        findNullable(
            OauthAuthorizationConsentId {
                this.registeredClientId = registeredClientId
                this.principalName = principalName
            },
        )

    /** 按注册客户端 ID 和主体名称删除 OAuth 授权同意 */
    fun deleteByRegisteredClientIdAndPrincipalName(
        registeredClientId: String,
        principalName: String,
    ) {
        deleteById(
            OauthAuthorizationConsentId {
                this.registeredClientId = registeredClientId
                this.principalName = principalName
            },
        )
    }
}
