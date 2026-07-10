package io.github.lishangbu.system.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 系统访问节点管理响应。
 */
@Schema(description = "系统访问节点响应。访问节点是菜单目录、路由页面和 API 权限的统一描述。")
@Immutable
interface AccessNodeResponse {
	@get:Schema(type = "string", description = "访问节点主键 ID。", example = "301")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(description = "访问节点稳定 code。角色绑定和前端权限判断都依赖该值。", example = "system.rbac.users")
	val code: String
	@get:Schema(description = "访问节点展示名称。", example = "用户管理")
	val name: String
	@get:Schema(description = "节点类型：DIRECTORY、ROUTE 或 API。", example = "ROUTE")
	val type: String
	@get:Schema(type = "string", description = "父访问节点 ID。根节点为空。", example = "300", nullable = true)
	@JsonConverter(LongToStringConverter::class)
	val parentId: Long?
	@get:Schema(description = "前端路由路径。仅菜单或路由节点通常有值。", example = "/system/rbac/users", nullable = true)
	val path: String?
	@get:Schema(description = "前端图标标识。", example = "lucide:users", nullable = true)
	val icon: String?
	@get:Schema(description = "同级节点排序值，数值越小越靠前。", example = "10")
	val sortOrder: Int
	@get:Schema(description = "是否在管理端菜单或路由中可见。API 权限通常不可见。", example = "true")
	val visible: Boolean
	@get:Schema(description = "节点是否启用。禁用节点不参与菜单和权限判定。", example = "true")
	val enabled: Boolean
	@get:Schema(description = "API 节点限制的 HTTP 方法。为空表示不按方法区分。", example = "GET", nullable = true)
	val apiMethod: String?
	@get:Schema(description = "API 节点匹配的后端路径模式。", example = "/api/system/**", nullable = true)
	val apiPattern: String?
}
