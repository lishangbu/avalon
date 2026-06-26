package io.github.lishangbu.system.rbac.role

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 更新 RBAC 角色的系统管理请求。
 */
@Schema(description = "更新 RBAC 角色请求。角色 code 不在该接口中修改。")
data class UpdateRoleRequest(
	@field:Schema(description = "新的角色名称。", example = "审计负责人")
	var name: String = "",
	@field:Schema(description = "角色更新后拥有的完整访问节点 code 集合。传空列表表示移除全部访问节点绑定。", example = "[\"security:admin\"]")
	var accessNodeCodes: List<String> = emptyList(),
)
