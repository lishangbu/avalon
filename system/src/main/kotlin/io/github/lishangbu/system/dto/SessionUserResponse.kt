package io.github.lishangbu.system.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 当前用户基础信息。
 */
@Schema(description = "当前登录用户基础信息。")
@Immutable
interface SessionUserResponse {
	@get:Schema(type = "string", description = "用户主键 ID。", example = "40001")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(description = "登录用户名。", example = "admin")
	val username: String
	@get:Schema(description = "管理端展示名称。", example = "系统管理员")
	val displayName: String
}
