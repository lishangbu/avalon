package io.github.lishangbu.system.oauth.client

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

/**
 * OAuth client 系统管理响应。
 *
 * 响应不返回 `clientSecret`，避免管理列表和日志链路泄露客户端凭据。
 */
@Schema(description = "OAuth client 系统管理响应。只包含客户端元数据，不包含 clientSecret。")
data class OAuthClientResponse(
	@field:Schema(description = "OAuth client 记录主键 ID。", example = "501")
	val id: Long,
	@field:Schema(description = "OAuth clientId。", example = "system-admin-jwt")
	val clientId: String,
	@field:Schema(description = "客户端展示名称。", example = "系统管理 JWT Client")
	val clientName: String,
	@field:Schema(description = "客户端认证方式集合。", example = "[\"client_secret_basic\"]")
	val clientAuthenticationMethods: List<String>,
	@field:Schema(description = "授权类型集合。", example = "[\"urn:security:params:oauth:grant-type:password\", \"refresh_token\"]")
	val authorizationGrantTypes: List<String>,
	@field:Schema(description = "允许请求的 scope 集合。", example = "[\"security:admin\"]")
	val scopes: List<String>,
	@field:Schema(description = "access token 格式。self-contained 表示 JWT，reference 表示 opaque/reference token。", example = "self-contained")
	val accessTokenFormat: String,
	@field:Schema(description = "access token 有效期，单位秒。", example = "3600")
	val accessTokenTtlSeconds: Long,
	@field:Schema(description = "refresh token 有效期，单位秒。", example = "7200")
	val refreshTokenTtlSeconds: Long,
	@field:Schema(description = "创建时间。", example = "2026-06-25T15:30:00Z")
	val createdAt: OffsetDateTime,
	@field:Schema(description = "最近更新时间。", example = "2026-06-25T15:35:00Z")
	val updatedAt: OffsetDateTime,
)
