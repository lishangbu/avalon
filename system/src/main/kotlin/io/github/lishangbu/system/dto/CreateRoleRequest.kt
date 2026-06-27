package io.github.lishangbu.system.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 创建 RBAC 角色的系统管理请求。
 */
@Schema(description = "创建 RBAC 角色请求。角色创建时同时写入完整访问节点绑定。")
data class CreateRoleRequest(
	@field:Schema(description = "角色稳定 code。创建后应视为长期契约。", example = "audit-admin")
	var code: String = "",
	@field:Schema(description = "角色名称。用于管理端展示。", example = "审计管理员")
	var name: String = "",
	@field:Schema(description = "角色创建后拥有的完整访问节点 code 集合。", example = "[\"security:admin\"]")
	var accessNodeCodes: List<String> = emptyList(),
)
