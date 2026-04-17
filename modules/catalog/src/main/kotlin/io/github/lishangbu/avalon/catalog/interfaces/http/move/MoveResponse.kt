package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.domain.Move
import io.github.lishangbu.avalon.catalog.interfaces.http.type.TypeDefinitionSummaryResponse
import io.github.lishangbu.avalon.catalog.interfaces.http.type.toResponse
import java.util.UUID

/**
 * 招式明细响应。
 *
 * @property id 招式主键。
 * @property code 招式业务码。
 * @property name 招式展示名称。
 * @property type 招式属性摘要。
 * @property categoryCode 招式伤害类别码。
 * @property moveCategory 招式分类摘要。
 * @property moveAilment 招式异常状态摘要。
 * @property moveTarget 招式目标摘要。
 * @property description 招式说明，可为空。
 * @property effectChance 追加效果概率，可为空。
 * @property power 招式威力，可为空。
 * @property accuracy 招式命中率，可为空。
 * @property powerPoints 招式基础 PP。
 * @property priority 招式优先级。
 * @property text 招式文本，可为空。
 * @property shortEffect 招式短效果说明，可为空。
 * @property effect 招式效果说明，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前招式是否启用。
 * @property version 乐观锁版本号。
 */
data class MoveResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val type: TypeDefinitionSummaryResponse,
    val categoryCode: String,
    val moveCategory: MoveCategorySummaryResponse?,
    val moveAilment: MoveAilmentSummaryResponse?,
    val moveTarget: MoveTargetSummaryResponse?,
    val description: String?,
    val effectChance: Int?,
    val power: Int?,
    val accuracy: Int?,
    val powerPoints: Int,
    val priority: Int,
    val text: String?,
    val shortEffect: String?,
    val effect: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 将招式领域对象转换为接口响应。
 *
 * @return 可直接返回给调用方的招式明细。
 */
fun Move.toResponse(): MoveResponse =
    MoveResponse(
        id = id.value,
        code = code,
        name = name,
        type = type.toResponse(),
        categoryCode = categoryCode.name,
        moveCategory = moveCategory?.toResponse(),
        moveAilment = moveAilment?.toResponse(),
        moveTarget = moveTarget?.toResponse(),
        description = description,
        effectChance = effectChance,
        power = power,
        accuracy = accuracy,
        powerPoints = powerPoints,
        priority = priority,
        text = text,
        shortEffect = shortEffect,
        effect = effect,
        sortingOrder = sortingOrder,
        enabled = enabled,
        version = version,
    )

