package io.github.lishangbu.avalon.catalog.interfaces.http.type

import io.github.lishangbu.avalon.catalog.domain.TypeDefinition
import java.util.UUID

/**
 * 属性定义明细响应。
 *
 * @property id 属性主键。
 * @property code 属性业务编码。
 * @property name 属性展示名称。
 * @property description 属性说明，可为空。
 * @property icon 前端图标标识，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前属性是否启用。
 * @property version 乐观锁版本号。
 */
data class TypeDefinitionResponse(
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
 * 将属性定义聚合转换为明细响应。
 */
fun TypeDefinition.toResponse(): TypeDefinitionResponse =
    TypeDefinitionResponse(
        id = id.value,
        code = code,
        name = name,
        description = description,
        icon = icon,
        sortingOrder = sortingOrder,
        enabled = enabled,
        version = version,
    )
