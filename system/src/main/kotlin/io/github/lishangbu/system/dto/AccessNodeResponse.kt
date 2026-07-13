package io.github.lishangbu.system.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 系统访问权限管理响应。
 */
@Schema(description = "系统访问权限响应。权限 code 是角色授权、令牌 authority 和客户端权限判断的共同契约。")
@Immutable
interface AccessNodeResponse {
	@get:Schema(type = "string", description = "访问节点主键 ID。", example = "301")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(description = "访问节点稳定 code。角色绑定和前端权限判断都依赖该值。", example = "system.rbac.users")
	val code: String
	@get:Schema(description = "权限管理展示名称。", example = "用户管理")
	val name: String
	@get:Schema(description = "权限是否启用。禁用权限不会进入新签发令牌或当前登录态权限集合。", example = "true")
	val enabled: Boolean
}
