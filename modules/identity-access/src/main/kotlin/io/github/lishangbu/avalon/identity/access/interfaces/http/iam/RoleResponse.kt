package io.github.lishangbu.avalon.identity.access.interfaces.http.iam

import io.github.lishangbu.avalon.identity.access.domain.iam.Role
import java.util.UUID

/**
 * 角色响应。
 *
 * @property id 角色主键。
 * @property code 角色业务编码。
 * @property name 角色展示名称。
 * @property enabled 角色是否启用。
 * @property menuIds 当前绑定的菜单主键列表。
 * @property permissionIds 当前绑定的权限主键列表。
 * @property version 乐观锁版本号。
 */
data class RoleResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val enabled: Boolean,
    val menuIds: List<UUID>,
    val permissionIds: List<UUID>,
    val version: Long,
)

/**
 * 将角色领域对象转换为接口响应。
 *
 * @return 面向管理端的角色视图。
 */
fun Role.toResponse(): RoleResponse =
    RoleResponse(
        id = id.value,
        code = code,
        name = name,
        enabled = enabled,
        menuIds = menuIds.map { it.value }.sorted(),
        permissionIds = permissionIds.map { it.value }.sorted(),
        version = version,
    )
