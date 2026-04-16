package io.github.lishangbu.avalon.identity.access.interfaces.http.iam

import io.github.lishangbu.avalon.identity.access.domain.iam.MenuId
import io.github.lishangbu.avalon.identity.access.domain.iam.PermissionId
import io.github.lishangbu.avalon.identity.access.domain.iam.RoleDraft
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * 角色创建或更新请求。
 *
 * @property code 角色业务编码。
 * @property name 角色展示名称。
 * @property enabled 角色是否启用。
 * @property menuIds 角色可访问的菜单主键集合。
 * @property permissionIds 角色直接绑定的权限主键集合。
 */
data class UpsertRoleRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val code: String,
    @field:NotBlank
    @field:Size(max = 128)
    val name: String,
    val enabled: Boolean = true,
    val menuIds: Set<UUID> = emptySet(),
    val permissionIds: Set<UUID> = emptySet(),
)

/**
 * 将角色请求转换为领域草稿。
 *
 * @return 已完成标识封装的角色草稿。
 */
fun UpsertRoleRequest.toDraft(): RoleDraft =
    RoleDraft(
        code = code.trim(),
        name = name.trim(),
        enabled = enabled,
        menuIds = menuIds.map(::MenuId).toSet(),
        permissionIds = permissionIds.map(::PermissionId).toSet(),
    )

