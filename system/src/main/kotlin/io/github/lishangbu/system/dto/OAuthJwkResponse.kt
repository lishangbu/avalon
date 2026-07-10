package io.github.lishangbu.system.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * JWK 系统管理响应。
 *
 * 只暴露 key 元数据，不返回包含私钥材料的 `jwkJson`。
 */
@Schema(description = "JWK 系统管理响应。只包含 key 元数据，不包含私钥材料。")
@Immutable
interface OAuthJwkResponse {
	@get:Schema(type = "string", description = "JWK 记录主键 ID。", example = "601")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(description = "JWK keyId。用于 JWT header 的 kid 和 key 管理检索。", example = "system-jwt-key-20260625")
	val keyId: String
	@get:Schema(description = "是否为当前签发 JWT 使用的 active key。", example = "true")
	val active: Boolean
}
