package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.domain.Nature
import java.util.UUID

/**
 * 性格明细响应。
 *
 * @property id 性格主键。
 * @property code 性格业务编码。
 * @property name 性格展示名称。
 * @property description 性格说明，可为空。
 * @property increasedStatCode 被该性格正向修正的数值编码；中性性格时为空。
 * @property decreasedStatCode 被该性格负向修正的数值编码；中性性格时为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前性格是否启用。
 * @property version 乐观锁版本号。
 */
data class NatureResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val increasedStatCode: String?,
    val decreasedStatCode: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 把性格定义转换为接口响应。
 *
 * @return 可直接返回给调用方的性格明细。
 */
fun Nature.toResponse(): NatureResponse =
    NatureResponse(
        id = id.value,
        code = code,
        name = name,
        description = description,
        increasedStatCode = increasedStatCode?.name,
        decreasedStatCode = decreasedStatCode?.name,
        sortingOrder = sortingOrder,
        enabled = enabled,
        version = version,
    )
