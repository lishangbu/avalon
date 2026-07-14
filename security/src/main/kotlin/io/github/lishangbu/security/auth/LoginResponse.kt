package io.github.lishangbu.security.auth

data class LoginResponse(
	val tokenName: String,
	val tokenValue: String,
	val timeout: Long,
)
