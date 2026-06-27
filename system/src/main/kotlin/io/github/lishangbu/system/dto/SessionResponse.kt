package io.github.lishangbu.system.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 当前登录态响应。
 */
@Schema(description = "当前登录态响应。管理端初始化用户、权限和菜单时使用。")
data class SessionResponse(
	@field:Schema(description = "当前登录用户基础信息。")
	val user: SessionUserResponse,
	@field:Schema(description = "当前用户角色快照。")
	val roles: List<SessionRoleResponse>,
	@field:Schema(description = "当前用户拥有的访问节点 code 快照。", example = "[\"security:admin\", \"system.rbac.users\"]")
	val accessNodeCodes: List<String>,
	@field:Schema(description = "管理端可渲染菜单树。只包含可见且启用的菜单/路由节点。")
	val menus: List<SessionMenuNodeResponse>,
)
