package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.domain.MoveLearnMethod
import java.util.UUID

/**
 * 招式学习方法明细响应。
 *
 * @property id 招式学习方法主键。
 * @property code 招式学习方法业务码。
 * @property name 招式学习方法展示名称。
 * @property description 招式学习方法说明，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前招式学习方法是否启用。
 * @property version 乐观锁版本号。
 */
data class MoveLearnMethodResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 将招式学习方法领域对象转换为接口响应。
 *
 * @return 可直接返回给调用方的招式学习方法明细。
 */
fun MoveLearnMethod.toResponse(): MoveLearnMethodResponse =
    MoveLearnMethodResponse(
        id = id.value,
        code = code,
        name = name,
        description = description,
        sortingOrder = sortingOrder,
        enabled = enabled,
        version = version,
    )
