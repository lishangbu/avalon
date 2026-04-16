package io.github.lishangbu.avalon.identity.access.interfaces.http.iam

import io.github.lishangbu.avalon.identity.access.domain.iam.MenuId
import io.github.lishangbu.avalon.identity.access.domain.iam.PermissionDraft
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * 权限创建或更新请求。
 *
 * @property menuId 权限所属菜单主键。
 * @property code 权限业务编码。
 * @property name 权限展示名称。
 * @property enabled 权限是否启用。
 * @property sortingOrder 权限排序值。
 */
data class UpsertPermissionRequest(
    @field:NotNull
    val menuId: UUID,
    @field:NotBlank
    @field:Size(max = 128)
    val code: String,
    @field:NotBlank
    @field:Size(max = 128)
    val name: String,
    val enabled: Boolean = true,
    val sortingOrder: Int = 0,
)

/**
 * 将权限请求转换为领域草稿。
 *
 * @return 可直接交给应用服务的权限草稿。
 */
fun UpsertPermissionRequest.toDraft(): PermissionDraft =
    PermissionDraft(
        menuId = MenuId(menuId),
        code = code.trim(),
        name = name.trim(),
        enabled = enabled,
        sortingOrder = sortingOrder,
    )

