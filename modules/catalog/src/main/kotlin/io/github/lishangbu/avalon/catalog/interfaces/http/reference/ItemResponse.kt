package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.domain.Item
import java.util.UUID

/**
 * 道具明细响应。
 *
 * @property id 道具主键。
 * @property code 道具业务编码。
 * @property name 道具展示名称。
 * @property categoryCode 道具分类业务码。
 * @property description 道具说明，可为空。
 * @property icon 图标标识，可为空。
 * @property maxStackSize 单格最大堆叠数量。
 * @property consumable 当前道具是否属于消耗型道具。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前道具是否启用。
 * @property version 乐观锁版本号。
 */
data class ItemResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val categoryCode: String,
    val description: String?,
    val icon: String?,
    val maxStackSize: Int,
    val consumable: Boolean,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 把道具定义转换为接口响应。
 *
 * @return 可直接返回给调用方的道具明细。
 */
fun Item.toResponse(): ItemResponse =
    ItemResponse(
        id = id.value,
        code = code,
        name = name,
        categoryCode = categoryCode,
        description = description,
        icon = icon,
        maxStackSize = maxStackSize,
        consumable = consumable,
        sortingOrder = sortingOrder,
        enabled = enabled,
        version = version,
    )
