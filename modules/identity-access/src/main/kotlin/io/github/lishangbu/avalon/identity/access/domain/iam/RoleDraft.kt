package io.github.lishangbu.avalon.identity.access.domain.iam

/**
 * 创建或更新角色时使用的输入草稿。
 *
 * @property code 角色业务编码。
 * @property name 角色展示名称。
 * @property enabled 角色是否启用。
 * @property menuIds 角色可访问的菜单标识集合。
 * @property permissionIds 角色直接绑定的权限标识集合。
 */
data class RoleDraft(
    val code: String,
    val name: String,
    val enabled: Boolean,
    val menuIds: Set<MenuId>,
    val permissionIds: Set<PermissionId>,
)