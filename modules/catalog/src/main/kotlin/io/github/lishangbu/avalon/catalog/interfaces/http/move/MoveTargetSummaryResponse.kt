package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.domain.MoveTargetSummary
import java.util.UUID

/**
 * 招式目标摘要响应。
 *
 * @property id 招式目标主键。
 * @property code 招式目标业务编码。
 * @property name 招式目标展示名称。
 */
data class MoveTargetSummaryResponse(
    val id: UUID,
    val code: String,
    val name: String,
)

/**
 * 将招式目标摘要转换为接口摘要响应。
 *
 * @return 可复用的招式目标轻量对象。
 */
fun MoveTargetSummary.toResponse(): MoveTargetSummaryResponse =
    MoveTargetSummaryResponse(
        id = id.value,
        code = code,
        name = name,
    )
