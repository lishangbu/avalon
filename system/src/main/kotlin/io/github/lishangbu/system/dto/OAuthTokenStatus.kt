package io.github.lishangbu.system.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * access token 生命周期状态。
 */
@Schema(description = "access token 生命周期状态。")
enum class OAuthTokenStatus {
	ACTIVE,
	EXPIRED,
	REVOKED,
	NO_ACCESS_TOKEN,
}
