package io.github.lishangbu.avalon.identity.access.domain.iam


/**
 * 角色聚合的领域视图。
 *
 * @property id 角色标识。
 * @property code 角色业务编码。
 * @property name 角色展示名称。
 * @property enabled 角色是否启用。
 * @property menuIds 当前绑定的菜单标识集合。
 * @property permissionIds 当前绑定的权限标识集合。
 * @property version 乐观锁版本号。
 */
data class Role(
    val id: RoleId,
    val code: String,
    val name: String,
    val enabled: Boolean,
    val menuIds: Set<MenuId>,
    val permissionIds: Set<PermissionId>,
    val version: Long,
)