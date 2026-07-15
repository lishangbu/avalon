package io.github.lishangbu.match.trainer

import io.github.lishangbu.security.event.AccountSecurityRevokedEvent
import io.github.lishangbu.security.event.AccountSessionEndedEvent
import io.github.lishangbu.match.event.PlayerEventHub
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/** 全局安全变更提交后清除 Trainer Session 与由该 Session 维护的 Presence；主动退出则即时清除。 */
@Component
class AccountTrainerSessionRevocationListener(private val sessions: TrainerSessionRegistry, private val events: PlayerEventHub) {
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	fun revoke(event: AccountSecurityRevokedEvent) { events.revokeAccount(event.accountId); sessions.leave(event.accountId) }

	@EventListener
	fun leave(event: AccountSessionEndedEvent) {
		if (sessions.leaveOwnedByLoginToken(event.accountId, event.loginToken)) {
			events.revokeAccount(event.accountId)
		}
	}
}
