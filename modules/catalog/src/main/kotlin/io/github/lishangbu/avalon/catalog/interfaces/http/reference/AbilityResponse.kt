package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.domain.Ability
import java.util.UUID

/**
 * 特性明细响应。
 *
 * @property id 特性主键。
 * @property code 特性业务编码。
 * @property name 特性展示名称。
 * @property description 特性说明，可为空。
 * @property icon 图标标识，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前特性是否启用。
 * @property version 乐观锁版本号。
 */
data class AbilityResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val icon: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 把特性定义转换为接口响应。
 *
 * @return 可直接返回给调用方的特性明细。
 */
fun Ability.toResponse(): AbilityResponse =
    AbilityResponse(
        id = id.value,
        code = code,
        name = name,
        description = description,
        icon = icon,
        sortingOrder = sortingOrder,
        enabled = enabled,
        version = version,
    )
