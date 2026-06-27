package io.github.lishangbu.system.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 更新 OAuth client 可管理字段的系统管理请求。
 */
@Schema(description = "更新 OAuth client 请求。该接口不修改 clientId 和 clientSecret。")
data class UpdateOAuthClientRequest(
	@field:Schema(description = "新的客户端展示名称。", example = "系统工具 Reference Client")
	var clientName: String = "",
	@field:Schema(description = "更新后的完整 scope 集合。", example = "[\"security:admin\"]")
	var scopes: List<String> = emptyList(),
	@field:Schema(description = "access token 格式。self-contained 表示 JWT，reference 表示 opaque/reference token。", example = "reference")
	var accessTokenFormat: String = "",
	@field:Schema(description = "access token 有效期，单位秒。", example = "900")
	var accessTokenTtlSeconds: Long = 0,
	@field:Schema(description = "refresh token 有效期，单位秒。", example = "3600")
	var refreshTokenTtlSeconds: Long = 0,
)
