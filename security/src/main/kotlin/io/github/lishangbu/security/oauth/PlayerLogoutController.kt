package io.github.lishangbu.security.oauth

import io.github.lishangbu.security.event.AccountSessionEndedEvent
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.web.bind.annotation.*

/** 普通退出只撤销当前 access token 所属 family，并广播账户游戏 Session 结束信号。 */
@RestController
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class PlayerLogoutController(
	private val authorizations: OAuth2AuthorizationService,
	private val events: ApplicationEventPublisher,
) {
	@PostMapping("/api/player/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun logout(authentication: Authentication, @RequestHeader("Authorization") authorizationHeader: String) {
		val token = authorizationHeader.removePrefix("Bearer ").trim()
		authorizations.findByToken(token, OAuth2TokenType.ACCESS_TOKEN)?.let(authorizations::remove)
		val accountId = authentication.principal.let { principal ->
			when (principal) {
				is org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal -> principal.getAttribute<String>("account_id")
				is org.springframework.security.oauth2.jwt.Jwt -> principal.getClaimAsString("account_id")
				else -> null
			}
		}?.toLongOrNull() ?: return
		events.publishEvent(AccountSessionEndedEvent(accountId))
	}
}
