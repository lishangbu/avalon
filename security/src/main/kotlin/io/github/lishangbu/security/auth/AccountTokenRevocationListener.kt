package io.github.lishangbu.security.auth

import cn.dev33.satoken.stp.StpUtil
import io.github.lishangbu.security.event.AccountSecurityRevokedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/** 在账号安全状态变化后注销该账号的全部登录。 */
@Component
class AccountTokenRevocationListener {
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
	fun revokeAfterCommit(event: AccountSecurityRevokedEvent) {
		StpUtil.getStpLogic().logout(event.accountId)
	}

	@EventListener(condition = "!T(org.springframework.transaction.support.TransactionSynchronizationManager).isActualTransactionActive()")
	fun revokeWithoutTransaction(event: AccountSecurityRevokedEvent) {
		StpUtil.getStpLogic().logout(event.accountId)
	}
}
