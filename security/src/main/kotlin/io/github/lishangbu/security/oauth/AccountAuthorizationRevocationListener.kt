package io.github.lishangbu.security.oauth

import io.github.lishangbu.security.entity.OAuth2AuthorizationRecord
import io.github.lishangbu.security.entity.principalName
import io.github.lishangbu.security.event.AccountSecurityRevokedEvent
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/** 账户安全状态变化时，同步删除其全部 OAuth authorization，也就是所有 token family。 */
@Component
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class AccountAuthorizationRevocationListener(private val sqlClient: KSqlClient) {
	@EventListener
	fun revoke(event: AccountSecurityRevokedEvent) {
		sqlClient.createDelete(OAuth2AuthorizationRecord::class) {
			where(table.principalName eq event.principalName)
		}.execute()
	}
}
