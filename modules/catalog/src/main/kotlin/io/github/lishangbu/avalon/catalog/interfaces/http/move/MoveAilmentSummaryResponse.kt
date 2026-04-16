package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.domain.MoveAilmentSummary
import java.util.UUID

/**
 * 招式异常状态摘要响应。
 *
 * @property id 招式异常状态主键。
 * @property code 招式异常状态业务码。
 * @property name 招式异常状态展示名称。
 */
data class MoveAilmentSummaryResponse(
    val id: UUID,
    val code: String,
    val name: String,
)

/**
 * 将招式异常状态摘要转换为接口摘要响应。
 */
fun MoveAilmentSummary.toResponse(): MoveAilmentSummaryResponse =
    MoveAilmentSummaryResponse(
        id = id.value,
        code = code,
        name = name,
    )
