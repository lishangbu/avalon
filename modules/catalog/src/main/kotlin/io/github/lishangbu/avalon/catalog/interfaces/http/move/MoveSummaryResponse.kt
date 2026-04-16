package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.domain.MoveSummary
import java.util.UUID

/**
 * 招式摘要响应。
 *
 * @property id 招式主键。
 * @property code 招式业务码。
 * @property name 招式展示名称。
 */
data class MoveSummaryResponse(
    val id: UUID,
    val code: String,
    val name: String,
)

/**
 * 将招式摘要转换为接口摘要响应。
 *
 * @return 可复用的招式轻量对象。
 */
fun MoveSummary.toResponse(): MoveSummaryResponse =
    MoveSummaryResponse(
        id = id.value,
        code = code,
        name = name,
    )
