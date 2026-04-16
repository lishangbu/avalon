package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.domain.MoveCategorySummary
import java.util.UUID

/**
 * 招式分类摘要响应。
 *
 * @property id 招式分类主键。
 * @property code 招式分类业务码。
 * @property name 招式分类展示名称。
 */
data class MoveCategorySummaryResponse(
    val id: UUID,
    val code: String,
    val name: String,
)

/**
 * 将招式分类摘要转换为接口摘要响应。
 */
fun MoveCategorySummary.toResponse(): MoveCategorySummaryResponse =
    MoveCategorySummaryResponse(
        id = id.value,
        code = code,
        name = name,
    )
