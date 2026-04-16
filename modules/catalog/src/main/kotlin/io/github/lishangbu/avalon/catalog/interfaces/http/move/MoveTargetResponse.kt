package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.domain.MoveTarget
import java.util.UUID

/**
 * 招式目标明细响应。
 *
 * @property id 招式目标主键。
 * @property code 招式目标业务编码。
 * @property name 招式目标展示名称。
 * @property description 招式目标说明，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前招式目标是否启用。
 * @property version 乐观锁版本号。
 */
data class MoveTargetResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 将招式目标领域对象转换为接口响应。
 *
 * @return 可直接返回给调用方的招式目标明细。
 */
fun MoveTarget.toResponse(): MoveTargetResponse =
    MoveTargetResponse(
        id = id.value,
        code = code,
        name = name,
        description = description,
        sortingOrder = sortingOrder,
        enabled = enabled,
        version = version,
    )
