package io.github.lishangbu.avalon.identity.access.domain.iam


/**
 * 权限聚合的领域视图。
 *
 * @property id 权限标识。
 * @property menuId 所属菜单标识。
 * @property code 权限业务编码。
 * @property name 权限展示名称。
 * @property enabled 权限是否启用。
 * @property sortingOrder 权限排序值。
 * @property version 乐观锁版本号。
 */
data class Permission(
    val id: PermissionId,
    val menuId: MenuId,
    val code: String,
    val name: String,
    val enabled: Boolean,
    val sortingOrder: Int,
    val version: Long,
)