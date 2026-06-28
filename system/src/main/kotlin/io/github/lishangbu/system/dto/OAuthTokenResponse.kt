package io.github.lishangbu.system.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

/**
 * OAuth 令牌管理响应。
 *
 * 响应只暴露授权记录和令牌生命周期元数据，不返回任何 token value。
 */
@Schema(description = "OAuth 令牌管理响应。只包含授权和令牌生命周期元数据，不包含 token 明文。")
data class OAuthTokenResponse(
	@field:Schema(description = "Spring Authorization Server 授权记录 ID。", example = "2f4b5b0f-ef8b-4e83-9df8-ec6fd8bd2b70")
	val id: String,
	@field:Schema(description = "注册客户端记录 ID。", example = "501")
	val registeredClientId: String,
	@field:Schema(description = "OAuth clientId。", example = "system-admin-opaque")
	val clientId: String?,
	@field:Schema(description = "OAuth 客户端展示名称。", example = "系统管理 Opaque Client")
	val clientName: String?,
	@field:Schema(description = "授权主体用户名。", example = "admin")
	val principalName: String,
	@field:Schema(description = "授权类型。", example = "urn:security:params:oauth:grant-type:password")
	val authorizationGrantType: String,
	@field:Schema(description = "授权记录允许的 scope。", example = "[\"security:admin\"]")
	val authorizedScopes: List<String>,
	@field:Schema(description = "access token 实际携带的 scope。", example = "[\"security:admin\"]")
	val accessTokenScopes: List<String>,
	@field:Schema(description = "access token 类型。", example = "Bearer")
	val accessTokenType: String?,
	@field:Schema(description = "access token 签发时间。")
	val accessTokenIssuedAt: OffsetDateTime?,
	@field:Schema(description = "access token 过期时间。")
	val accessTokenExpiresAt: OffsetDateTime?,
	@field:Schema(description = "refresh token 签发时间。")
	val refreshTokenIssuedAt: OffsetDateTime?,
	@field:Schema(description = "refresh token 过期时间。")
	val refreshTokenExpiresAt: OffsetDateTime?,
	@field:Schema(description = "令牌状态。", example = "ACTIVE")
	val status: OAuthTokenStatus,
	@field:Schema(description = "access token 当前是否仍可用。", example = "true")
	val active: Boolean,
)
