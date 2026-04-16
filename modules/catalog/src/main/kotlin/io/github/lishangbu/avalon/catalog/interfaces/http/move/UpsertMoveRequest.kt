package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.domain.*
import jakarta.validation.constraints.*
import java.util.*
import java.util.UUID

/**
 * 创建或更新招式定义时使用的请求体。
 *
 * @property code 招式业务码，映射时会统一转成大写。
 * @property name 招式展示名称。
 * @property typeDefinitionId 招式属性主键。
 * @property categoryCode 招式伤害类别业务码，映射时会统一转成大写。
 * @property moveCategoryId 招式分类主键，可为空。
 * @property moveAilmentId 招式异常状态主键，可为空。
 * @property moveTargetId 招式目标主键，可为空。
 * @property description 招式说明，可为空。
 * @property effectChance 追加效果概率，可为空。
 * @property power 招式威力，可为空。
 * @property accuracy 招式命中率，可为空。
 * @property powerPoints 招式基础 PP，必须大于 0。
 * @property priority 招式优先级。
 * @property text 招式文本，可为空。
 * @property shortEffect 招式短效果说明，可为空。
 * @property effect 招式效果说明，可为空。
 * @property sortingOrder 列表展示顺序，数值越小越靠前。
 * @property enabled 当前招式是否启用。
 */
data class UpsertMoveRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val code: String,
    @field:NotBlank
    @field:Size(max = 128)
    val name: String,
    val typeDefinitionId: UUID,
    @field:NotBlank
    @field:Size(max = 32)
    val categoryCode: String,
    val moveCategoryId: UUID? = null,
    val moveAilmentId: UUID? = null,
    val moveTargetId: UUID? = null,
    @field:Size(max = 1000)
    val description: String? = null,
    @field:Min(0)
    @field:Max(100)
    val effectChance: Int? = null,
    @field:PositiveOrZero
    val power: Int? = null,
    @field:Min(1)
    @field:Max(100)
    val accuracy: Int? = null,
    @field:Positive
    val powerPoints: Int,
    val priority: Int = 0,
    @field:Size(max = 1000)
    val text: String? = null,
    @field:Size(max = 1000)
    val shortEffect: String? = null,
    @field:Size(max = 1000)
    val effect: String? = null,
    val sortingOrder: Int = 0,
    val enabled: Boolean = true,
)

/**
 * 将招式请求体转换为领域草稿。
 *
 * @return 可直接交给应用服务写入的招式草稿。
 * @throws IllegalArgumentException 当 categoryCode 不受支持时抛出。
 */
fun UpsertMoveRequest.toDraft(): MoveDraft =
    MoveDraft(
        code = code.trim().uppercase(Locale.ROOT),
        name = name.trim(),
        typeDefinitionId = TypeDefinitionId(typeDefinitionId),
        categoryCode = categoryCode.toMoveCategoryCode(),
        moveCategoryId = moveCategoryId?.let(::MoveCategoryId),
        moveAilmentId = moveAilmentId?.let(::MoveAilmentId),
        moveTargetId = moveTargetId?.let(::MoveTargetId),
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        effectChance = effectChance,
        power = power,
        accuracy = accuracy,
        powerPoints = powerPoints,
        priority = priority,
        text = text?.trim()?.takeIf { it.isNotEmpty() },
        shortEffect = shortEffect?.trim()?.takeIf { it.isNotEmpty() },
        effect = effect?.trim()?.takeIf { it.isNotEmpty() },
        sortingOrder = sortingOrder,
        enabled = enabled,
    )

private fun String.toMoveCategoryCode(): MoveCategoryCode {
    val normalized = trim().uppercase(Locale.ROOT)
    return try {
        MoveCategoryCode.valueOf(normalized)
    } catch (_: IllegalArgumentException) {
        val supportedCodes = MoveCategoryCode.values().joinToString(", ") { it.name }
        throw CatalogBadRequest("categoryCode must be one of: $supportedCodes.")
    }
}

