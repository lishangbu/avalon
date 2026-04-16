package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.domain.MoveAilment
import java.util.UUID

/**
 * 招式异常状态明细响应。
 *
 * @property id 招式异常状态主键。
 * @property code 招式异常状态业务码。
 * @property name 招式异常状态展示名称。
 * @property description 招式异常状态说明，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前招式异常状态是否启用。
 * @property version 乐观锁版本号。
 */
data class MoveAilmentResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 将招式异常状态领域对象转换为接口响应。
 */
fun MoveAilment.toResponse(): MoveAilmentResponse =
    MoveAilmentResponse(
        id = id.value,
        code = code,
        name = name,
        description = description,
        sortingOrder = sortingOrder,
        enabled = enabled,
        version = version,
    )
