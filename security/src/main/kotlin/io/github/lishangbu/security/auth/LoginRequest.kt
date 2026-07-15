package io.github.lishangbu.security.auth

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
	@field:NotBlank var username: String = "",
	@field:NotBlank
	@field:Size(min = 8, max = 128)
	var password: String = "",
)
