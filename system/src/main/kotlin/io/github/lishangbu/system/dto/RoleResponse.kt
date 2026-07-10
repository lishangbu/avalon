package io.github.lishangbu.system.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * RBAC 角色系统管理响应。
 */
@Schema(description = "RBAC 角色系统管理响应。包含角色基础信息和完整访问节点绑定快照。")
@Immutable
interface RoleResponse {
	@get:Schema(type = "string", description = "角色主键 ID。", example = "201")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(description = "角色稳定 code。用于用户绑定、权限排查和前端展示。", example = "system-admin")
	val code: String
	@get:Schema(description = "角色名称。", example = "系统管理员")
	val name: String
	@get:Schema(description = "角色当前绑定的完整访问节点 code 集合。", example = "[\"security:admin\"]")
	val accessNodeCodes: List<String>
}
