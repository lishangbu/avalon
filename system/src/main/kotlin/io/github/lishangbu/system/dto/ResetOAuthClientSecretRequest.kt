package io.github.lishangbu.system.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 重置 OAuth client secret 的系统管理请求。
 */
@Schema(description = "重置 OAuth client secret 请求。新 secret 提交后无法通过管理接口读回。")
data class ResetOAuthClientSecretRequest(
	@field:Schema(description = "新的 OAuth client 原始 secret。服务端会生成不可逆摘要。", example = "tools-secret-2026-v2", writeOnly = true, nullable = true)
	var clientSecret: String? = null,
)
