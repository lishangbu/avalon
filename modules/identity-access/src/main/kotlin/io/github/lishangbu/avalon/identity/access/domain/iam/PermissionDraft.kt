package io.github.lishangbu.avalon.identity.access.domain.iam

/**
 * 创建或更新权限时使用的输入草稿。
 *
 * @property menuId 权限所属菜单标识。
 * @property code 权限业务编码。
 * @property name 权限展示名称。
 * @property enabled 权限是否启用。
 * @property sortingOrder 权限排序值。
 */
data class PermissionDraft(
    val menuId: MenuId,
    val code: String,
    val name: String,
    val enabled: Boolean,
    val sortingOrder: Int,
)