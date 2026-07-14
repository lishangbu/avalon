package io.github.lishangbu.security.auth

import cn.dev33.satoken.stp.StpUtil
import io.github.lishangbu.security.entity.accountNonLocked
import io.github.lishangbu.security.entity.enabled
import io.github.lishangbu.security.entity.id
import io.github.lishangbu.security.entity.passwordHash
import io.github.lishangbu.security.rbac.JimmerSaTokenRbacService
import io.github.lishangbu.security.event.AccountSessionEndedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

/** 编排账号校验和 Sa-Token 登录生命周期。 */
@Service
class SaTokenAuthenticationService(
	private val rbacService: JimmerSaTokenRbacService,
	private val passwordEncoder: PasswordEncoder,
	private val events: ApplicationEventPublisher,
) {
	fun login(request: LoginRequest): LoginResponse {
		val user = rbacService.findUserByUsername(request.username.trim())
		if (
			user == null ||
			!user.enabled ||
			!user.accountNonLocked ||
			!passwordEncoder.matches(request.password, user.passwordHash)
		) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误")
		}

		StpUtil.login(user.id)
		val tokenInfo = StpUtil.getTokenInfo()
		return LoginResponse(
			tokenName = tokenInfo.tokenName,
			tokenValue = tokenInfo.tokenValue,
			timeout = tokenInfo.tokenTimeout,
		)
	}

	fun logout() {
		val accountId = StpUtil.getLoginIdAsLong()
		StpUtil.logout()
		events.publishEvent(AccountSessionEndedEvent(accountId))
	}
}
