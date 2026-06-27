package io.github.lishangbu.system.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 当前用户角色快照。
 */
@Schema(description = "当前登录用户角色快照。")
data class SessionRoleResponse(
	@field:Schema(description = "角色稳定 code。", example = "system-admin")
	val code: String,
	@field:Schema(description = "角色名称。", example = "系统管理员")
	val name: String,
)
