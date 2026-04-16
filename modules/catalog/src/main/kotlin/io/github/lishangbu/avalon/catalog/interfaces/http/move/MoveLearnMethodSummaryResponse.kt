package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.domain.MoveLearnMethodSummary
import java.util.UUID

/**
 * 招式学习方法摘要响应。
 *
 * @property id 招式学习方法主键。
 * @property code 招式学习方法业务码。
 * @property name 招式学习方法展示名称。
 */
data class MoveLearnMethodSummaryResponse(
    val id: UUID,
    val code: String,
    val name: String,
)

/**
 * 将招式学习方法摘要转换为接口摘要响应。
 *
 * @return 可复用的招式学习方法轻量对象。
 */
fun MoveLearnMethodSummary.toResponse(): MoveLearnMethodSummaryResponse =
    MoveLearnMethodSummaryResponse(
        id = id.value,
        code = code,
        name = name,
    )
