package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.domain.MoveCategory
import java.util.UUID

/**
 * 招式分类明细响应。
 *
 * @property id 招式分类主键。
 * @property code 招式分类业务码。
 * @property name 招式分类展示名称。
 * @property description 招式分类说明，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前招式分类是否启用。
 * @property version 乐观锁版本号。
 */
data class MoveCategoryResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 将招式分类领域对象转换为接口响应。
 */
fun MoveCategory.toResponse(): MoveCategoryResponse =
    MoveCategoryResponse(
        id = id.value,
        code = code,
        name = name,
        description = description,
        sortingOrder = sortingOrder,
        enabled = enabled,
        version = version,
    )
