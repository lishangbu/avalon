package io.github.lishangbu.avalon.catalog.interfaces.http.type

import io.github.lishangbu.avalon.catalog.domain.TypeDefinitionSummary
import java.util.UUID

/**
 * 属性摘要响应。
 *
 * 该响应用于在更大的对象里内嵌展示属性身份，避免重复展开完整明细。
 *
 * @property id 属性主键。
 * @property code 属性业务编码。
 * @property name 属性展示名称。
 */
data class TypeDefinitionSummaryResponse(
    val id: UUID,
    val code: String,
    val name: String,
)

/**
 * 将属性摘要转换为 HTTP 响应。
 */
fun TypeDefinitionSummary.toResponse(): TypeDefinitionSummaryResponse =
    TypeDefinitionSummaryResponse(
        id = id.value,
        code = code,
        name = name,
    )
