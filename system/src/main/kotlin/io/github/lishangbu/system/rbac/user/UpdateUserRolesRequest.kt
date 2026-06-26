package io.github.lishangbu.system.rbac.user

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 更新 RBAC 用户角色绑定的系统管理请求。
 */
@Schema(description = "更新用户角色绑定请求。roleCodes 是更新后的完整角色集合。")
data class UpdateUserRolesRequest(
	@field:Schema(description = "用户更新后绑定的完整角色 code 集合。传空列表表示移除全部角色。", example = "[\"system-admin\"]")
	var roleCodes: List<String> = emptyList(),
)
