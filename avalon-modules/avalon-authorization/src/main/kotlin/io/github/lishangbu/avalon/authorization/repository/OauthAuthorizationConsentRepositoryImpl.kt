package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

@Repository
class OauthAuthorizationConsentRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : OauthAuthorizationConsentRepositoryExt {
    /** 按注册客户端 ID 和主体名称查询 OAuth 授权同意 */
    override fun findByRegisteredClientIdAndPrincipalName(
        registeredClientId: String,
        principalName: String,
    ): OauthAuthorizationConsent? =
        sql.findById(
            OauthAuthorizationConsent::class,
            OauthAuthorizationConsentId {
                this.registeredClientId = registeredClientId
                this.principalName = principalName
            },
        )

    /** 按注册客户端 ID 和主体名称删除 OAuth 授权同意 */
    override fun deleteByRegisteredClientIdAndPrincipalName(
        registeredClientId: String,
        principalName: String,
    ) {
        sql
            .createDelete(OauthAuthorizationConsent::class) {
                where(table.id.registeredClientId eq registeredClientId)
                where(table.id.principalName eq principalName)
                disableDissociation()
            }.execute()
    }
}
