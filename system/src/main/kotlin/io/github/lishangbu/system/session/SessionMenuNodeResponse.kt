package io.github.lishangbu.system.session

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 管理端菜单树节点。
 */
@Schema(description = "管理端菜单树节点。树结构已由服务端按访问节点父子关系和排序值整理。")
data class SessionMenuNodeResponse(
	@field:Schema(description = "访问节点稳定 code。", example = "system.rbac.users")
	val code: String,
	@field:Schema(description = "菜单或路由展示名称。", example = "用户管理")
	val name: String,
	@field:Schema(description = "节点类型，通常为 MENU 或 ROUTE。", example = "ROUTE")
	val type: String,
	@field:Schema(description = "前端路由路径。", example = "/system/rbac/users", nullable = true)
	val path: String?,
	@field:Schema(description = "前端组件标识。", example = "system/rbac/users", nullable = true)
	val componentKey: String?,
	@field:Schema(description = "前端图标标识。", example = "Users", nullable = true)
	val icon: String?,
	@field:Schema(description = "同级排序值，数值越小越靠前。", example = "10")
	val sortOrder: Int,
	@field:Schema(description = "子菜单或子路由节点。")
	val children: List<SessionMenuNodeResponse>,
)
