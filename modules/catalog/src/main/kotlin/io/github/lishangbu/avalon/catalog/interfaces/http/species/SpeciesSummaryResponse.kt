package io.github.lishangbu.avalon.catalog.interfaces.http.species

import io.github.lishangbu.avalon.catalog.domain.SpeciesSummary
import java.util.UUID

/**
 * 物种摘要响应。
 *
 * @property id 物种主键。
 * @property code 物种业务编码。
 * @property dexNumber 图鉴号。
 * @property name 物种展示名称。
 */
data class SpeciesSummaryResponse(
    val id: UUID,
    val code: String,
    val dexNumber: Int,
    val name: String,
)

/**
 * 把物种摘要转换为 HTTP 响应。
 *
 * @return 面向接口层的物种摘要视图。
 */
fun SpeciesSummary.toResponse(): SpeciesSummaryResponse =
    SpeciesSummaryResponse(
        id = id.value,
        code = code,
        dexNumber = dexNumber,
        name = name,
    )
