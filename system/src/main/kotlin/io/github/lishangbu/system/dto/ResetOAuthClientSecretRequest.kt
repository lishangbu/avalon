package io.github.lishangbu.system.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 重置 OAuth client secret 的系统管理请求。
 */
@Schema(description = "重置 OAuth client secret 请求。新 secret 提交后无法通过管理接口读回。")
data class ResetOAuthClientSecretRequest(
	@field:Schema(description = "新的 OAuth client secret。", example = "{noop}tools-secret-v2", writeOnly = true, nullable = true)
	var clientSecret: String? = null,
)
