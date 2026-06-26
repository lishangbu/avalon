package io.github.lishangbu.system.rbac.user

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

/**
 * RBAC 用户系统管理响应。
 *
 * 响应不返回密码摘要，只返回账号状态和角色绑定。
 */
@Schema(description = "RBAC 用户系统管理响应。用于列表、详情和状态变更后的统一用户快照。")
data class UserResponse(
	@field:Schema(description = "用户主键 ID。", example = "40001")
	val id: Long,
	@field:Schema(description = "登录用户名。", example = "auditor")
	val username: String,
	@field:Schema(description = "管理端展示名称。", example = "审计员")
	val displayName: String,
	@field:Schema(description = "账号是否启用。禁用账号无法通过密码授权换取新 token。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "账号是否未锁定。锁定账号无法通过密码授权换取新 token。", example = "true")
	val accountNonLocked: Boolean,
	@field:Schema(description = "用户当前绑定的完整角色 code 集合。", example = "[\"system-admin\"]")
	val roleCodes: List<String>,
	@field:Schema(description = "创建时间，使用服务端 OffsetDateTime 序列化格式。", example = "2026-06-25T15:30:00Z")
	val createdAt: OffsetDateTime,
	@field:Schema(description = "最近更新时间，状态、密码或角色绑定变化都会刷新。", example = "2026-06-25T15:35:00Z")
	val updatedAt: OffsetDateTime,
)
