package io.github.lishangbu.system.oauth.jwk

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

/**
 * JWK 系统管理响应。
 *
 * 只暴露 key 元数据，不返回包含私钥材料的 `jwkJson`。
 */
@Schema(description = "JWK 系统管理响应。只包含 key 元数据，不包含私钥材料。")
data class OAuthJwkResponse(
	@field:Schema(description = "JWK 记录主键 ID。", example = "601")
	val id: Long,
	@field:Schema(description = "JWK keyId。用于 JWT header 的 kid 和 key 管理检索。", example = "system-jwt-key-20260625")
	val keyId: String,
	@field:Schema(description = "是否为当前签发 JWT 使用的 active key。", example = "true")
	val active: Boolean,
	@field:Schema(description = "创建时间。", example = "2026-06-25T15:30:00Z")
	val createdAt: OffsetDateTime,
	@field:Schema(description = "最近更新时间。轮换 key 状态时会刷新。", example = "2026-06-25T15:35:00Z")
	val updatedAt: OffsetDateTime,
)
