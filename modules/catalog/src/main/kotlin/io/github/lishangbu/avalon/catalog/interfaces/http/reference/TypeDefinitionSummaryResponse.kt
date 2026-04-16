package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.domain.TypeDefinitionSummary
import java.util.UUID

/**
 * 类型摘要响应。
 *
 * 该响应用于在更大的对象里内嵌展示类型身份，避免重复展开完整明细。
 *
 * @property id 类型主键。
 * @property code 类型业务编码。
 * @property name 类型展示名称。
 */
data class TypeDefinitionSummaryResponse(
    val id: UUID,
    val code: String,
    val name: String,
)

/**
 * 将类型摘要转换为 HTTP 响应。
 *
 * @return 面向接口层的类型摘要视图。
 */
fun TypeDefinitionSummary.toResponse(): TypeDefinitionSummaryResponse =
    TypeDefinitionSummaryResponse(
        id = id.value,
        code = code,
        name = name,
    )
