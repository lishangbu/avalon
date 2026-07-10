package io.github.lishangbu.system.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * RBAC 用户系统管理响应。
 *
 * 响应不返回密码摘要，只返回账号状态和角色绑定。
 */
@Schema(description = "RBAC 用户系统管理响应。用于列表、详情和状态变更后的统一用户快照。")
@Immutable
interface UserResponse {
	@get:Schema(type = "string", description = "用户主键 ID。", example = "40001")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(description = "登录用户名。", example = "auditor")
	val username: String
	@get:Schema(description = "管理端展示名称。", example = "审计员")
	val displayName: String
	@get:Schema(description = "账号是否启用。禁用账号无法通过密码授权换取新 token。", example = "true")
	val enabled: Boolean
	@get:Schema(description = "账号是否未锁定。锁定账号无法通过密码授权换取新 token。", example = "true")
	val accountNonLocked: Boolean
	@get:Schema(description = "用户当前绑定的完整角色 code 集合。", example = "[\"system-admin\"]")
	val roleCodes: List<String>
}
