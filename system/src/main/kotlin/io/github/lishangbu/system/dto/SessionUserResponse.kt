package io.github.lishangbu.system.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 当前用户基础信息。
 */
@Schema(description = "当前登录用户基础信息。")
data class SessionUserResponse(
	@field:Schema(description = "用户主键 ID。", example = "40001")
	val id: Long,
	@field:Schema(description = "登录用户名。", example = "admin")
	val username: String,
	@field:Schema(description = "管理端展示名称。", example = "系统管理员")
	val displayName: String,
)
