package io.github.lishangbu.security.auth

import jakarta.validation.Valid
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** 第一方 Web 客户端登录和注销接口。 */
@RestController
@RequestMapping("/api/auth")
class AuthenticationController(
	private val service: SaTokenAuthenticationService,
	private val rateLimiter: LoginRateLimiter,
) {
	@PostMapping("/login")
	fun login(@Valid @RequestBody request: LoginRequest, servletRequest: HttpServletRequest): LoginResponse {
		if (!rateLimiter.tryAcquire(servletRequest.remoteAddr ?: "unknown", request.username)) {
			throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts")
		}
		return service.login(request)
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun logout() = service.logout()
}
