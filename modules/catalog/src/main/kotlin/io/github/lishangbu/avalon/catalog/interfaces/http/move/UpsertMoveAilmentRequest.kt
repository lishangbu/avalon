package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.domain.MoveAilmentDraft
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

/**
 * 创建或更新招式异常状态时使用的请求体。
 *
 * @property code 招式异常状态业务码，映射时会统一转成大写。
 * @property name 招式异常状态展示名称。
 * @property description 招式异常状态说明，可为空。
 * @property sortingOrder 列表展示顺序，数值越小越靠前。
 * @property enabled 当前招式异常状态是否启用。
 */
data class UpsertMoveAilmentRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val code: String,
    @field:NotBlank
    @field:Size(max = 128)
    val name: String,
    @field:Size(max = 1000)
    val description: String? = null,
    val sortingOrder: Int = 0,
    val enabled: Boolean = true,
)

/**
 * 将招式异常状态请求体转换为领域草稿。
 */
fun UpsertMoveAilmentRequest.toDraft(): MoveAilmentDraft =
    MoveAilmentDraft(
        code = code.trim().uppercase(Locale.ROOT),
        name = name.trim(),
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        sortingOrder = sortingOrder,
        enabled = enabled,
    )