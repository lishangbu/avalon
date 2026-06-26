package io.github.lishangbu.system.rbac.user

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 创建 RBAC 用户的系统管理请求。
 */
@Schema(description = "创建 RBAC 用户请求。密码只用于写入，不会在任何响应中返回。")
data class CreateUserRequest(
	@field:Schema(description = "登录用户名。必须唯一，建议使用稳定英文、数字、短横线或下划线组合。", example = "auditor")
	var username: String = "",
	@field:Schema(description = "初始密码。服务端会写入编码后的密码摘要，响应不会回显。", example = "secret123", writeOnly = true)
	var password: String = "",
	@field:Schema(description = "管理端展示名称。", example = "审计员")
	var displayName: String = "",
	@field:Schema(description = "创建后用户绑定的完整角色 code 集合。", example = "[\"system-admin\"]")
	var roleCodes: List<String> = emptyList(),
)
