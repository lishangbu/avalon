package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.domain.AbilitySummary
import java.util.UUID

/**
 * 特性摘要响应。
 *
 * @property id 特性主键。
 * @property code 特性业务编码。
 * @property name 特性展示名称。
 */
data class AbilitySummaryResponse(
    val id: UUID,
    val code: String,
    val name: String,
)

/**
 * 把特性摘要转换为接口响应。
 *
 * @return 可直接返回给调用方的特性摘要。
 */
fun AbilitySummary.toResponse(): AbilitySummaryResponse =
    AbilitySummaryResponse(
        id = id.value,
        code = code,
        name = name,
    )
