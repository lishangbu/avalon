package io.github.lishangbu.system.rbac.role

import io.swagger.v3.oas.annotations.media.Schema

/**
 * RBAC 角色系统管理响应。
 */
@Schema(description = "RBAC 角色系统管理响应。包含角色基础信息和完整访问节点绑定快照。")
data class RoleResponse(
	@field:Schema(description = "角色主键 ID。", example = "201")
	val id: Long,
	@field:Schema(description = "角色稳定 code。用于用户绑定、权限排查和前端展示。", example = "system-admin")
	val code: String,
	@field:Schema(description = "角色名称。", example = "系统管理员")
	val name: String,
	@field:Schema(description = "角色当前绑定的完整访问节点 code 集合。", example = "[\"security:admin\"]")
	val accessNodeCodes: List<String>,
)
