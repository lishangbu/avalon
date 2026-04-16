package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.domain.GrowthRateSummary
import java.util.UUID

/**
 * 成长率摘要响应。
 *
 * @property id 成长率主键。
 * @property code 成长率业务编码。
 * @property name 成长率展示名称。
 * @property formulaCode 经验公式业务码。
 */
data class GrowthRateSummaryResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val formulaCode: String,
)

/**
 * 把成长率摘要转换为 HTTP 响应。
 *
 * @return 面向接口层的成长率摘要视图。
 */
fun GrowthRateSummary.toResponse(): GrowthRateSummaryResponse =
    GrowthRateSummaryResponse(
        id = id.value,
        code = code,
        name = name,
        formulaCode = formulaCode.name,
    )
