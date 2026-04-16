package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.domain.ItemDraft
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.util.*

/**
 * 创建或更新道具定义时使用的请求体。
 *
 * @property code 道具业务编码；映射到领域草稿时会统一转成大写。
 * @property name 道具展示名称。
 * @property categoryCode 道具分类业务码；映射时会统一转成大写。
 * @property description 道具说明，可为空。
 * @property icon 前端展示时使用的图标标识，可为空。
 * @property maxStackSize 单格最大堆叠数量，必须大于 0。
 * @property consumable 当前道具是否属于消耗型道具。
 * @property sortingOrder 列表展示顺序，值越小越靠前。
 * @property enabled 当前道具是否启用。
 */
data class UpsertItemRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val code: String,
    @field:NotBlank
    @field:Size(max = 128)
    val name: String,
    @field:NotBlank
    @field:Size(max = 64)
    val categoryCode: String,
    @field:Size(max = 1000)
    val description: String? = null,
    @field:Size(max = 255)
    val icon: String? = null,
    @field:Positive
    val maxStackSize: Int = 1,
    val consumable: Boolean = false,
    val sortingOrder: Int = 0,
    val enabled: Boolean = true,
)

/**
 * 把道具请求转换为领域草稿。
 *
 * @return 可直接交给应用服务写入的道具草稿。
 */
fun UpsertItemRequest.toDraft(): ItemDraft =
    ItemDraft(
        code = code.trim().uppercase(Locale.ROOT),
        name = name.trim(),
        categoryCode = categoryCode.trim().uppercase(Locale.ROOT),
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        icon = icon?.trim()?.takeIf { it.isNotEmpty() },
        maxStackSize = maxStackSize,
        consumable = consumable,
        sortingOrder = sortingOrder,
        enabled = enabled,
    )