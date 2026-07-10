package io.github.lishangbu.system.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * OAuth client 系统管理响应。
 *
 * 响应不返回 `clientSecret`，避免管理列表和日志链路泄露客户端凭据。
 */
@Schema(description = "OAuth client 系统管理响应。只包含客户端元数据，不包含 clientSecret。")
@Immutable
interface OAuthClientResponse {
	@get:Schema(type = "string", description = "OAuth client 记录主键 ID。", example = "501")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(description = "OAuth clientId。", example = "system-admin-jwt")
	val clientId: String
	@get:Schema(description = "客户端展示名称。", example = "系统管理 JWT Client")
	val clientName: String
	@get:Schema(description = "客户端认证方式集合。", example = "[\"client_secret_basic\"]")
	val clientAuthenticationMethods: List<String>
	@get:Schema(description = "授权类型集合。", example = "[\"urn:security:params:oauth:grant-type:password\", \"refresh_token\"]")
	val authorizationGrantTypes: List<String>
	@get:Schema(description = "允许请求的 scope 集合。", example = "[\"security:admin\"]")
	val scopes: List<String>
	@get:Schema(description = "access token 格式。self-contained 表示 JWT，reference 表示 opaque/reference token。", example = "self-contained")
	val accessTokenFormat: String
	@get:Schema(description = "access token 有效期，单位秒。", example = "3600")
	val accessTokenTtlSeconds: Long
	@get:Schema(description = "refresh token 有效期，单位秒。", example = "7200")
	val refreshTokenTtlSeconds: Long
}
