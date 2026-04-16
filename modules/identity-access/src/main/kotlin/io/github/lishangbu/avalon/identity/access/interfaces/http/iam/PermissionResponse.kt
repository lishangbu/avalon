package io.github.lishangbu.avalon.identity.access.interfaces.http.iam

import io.github.lishangbu.avalon.identity.access.domain.iam.Permission
import java.util.UUID

/**
 * 权限响应。
 *
 * @property id 权限主键。
 * @property menuId 所属菜单主键。
 * @property code 权限业务编码。
 * @property name 权限展示名称。
 * @property enabled 权限是否启用。
 * @property sortingOrder 权限排序值。
 * @property version 乐观锁版本号。
 */
data class PermissionResponse(
    val id: UUID,
    val menuId: UUID,
    val code: String,
    val name: String,
    val enabled: Boolean,
    val sortingOrder: Int,
    val version: Long,
)

/**
 * 将权限领域对象转换为接口响应。
 *
 * @return 面向管理端的权限视图。
 */
fun Permission.toResponse(): PermissionResponse =
    PermissionResponse(
        id = id.value,
        menuId = menuId.value,
        code = code,
        name = name,
        enabled = enabled,
        sortingOrder = sortingOrder,
        version = version,
    )
