package io.github.lishangbu.security.auth

import cn.dev33.satoken.stp.StpUtil
import io.github.lishangbu.common.web.requiredPassword
import io.github.lishangbu.common.web.requiredUsername
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
		val username = request.username.requiredUsername("username")
		val password = request.password.requiredPassword("password")
		val user = rbacService.findUserByUsername(username)
		val passwordMatches = passwordEncoder.matches(password, user?.passwordHash ?: DUMMY_PASSWORD_HASH)
		if (
			user == null ||
			!user.enabled ||
			!user.accountNonLocked ||
			!passwordMatches
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
		val loginToken = StpUtil.getTokenValue()
		StpUtil.logout()
		events.publishEvent(AccountSessionEndedEvent(accountId, loginToken))
	}

	private companion object {
		const val DUMMY_PASSWORD_HASH =
			"{bcrypt}\$2a\$12\$OPymRqyndBpSY1l9izVj5uSrVXl9rUxsgRNMCSWWRk8PNGwH5tEYW"
	}
}
