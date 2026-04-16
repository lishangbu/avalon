package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.domain.MoveLearnMethodDraft
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

/**
 * 创建或更新招式学习方法时使用的请求体。
 *
 * @property code 招式学习方法业务码，映射时会统一转成大写。
 * @property name 招式学习方法展示名称。
 * @property description 招式学习方法说明，可为空。
 * @property sortingOrder 列表展示顺序，数值越小越靠前。
 * @property enabled 当前招式学习方法是否启用。
 */
data class UpsertMoveLearnMethodRequest(
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
 * 将招式学习方法请求体转换为领域草稿。
 *
 * @return 可直接交给应用服务写入的招式学习方法草稿。
 */
fun UpsertMoveLearnMethodRequest.toDraft(): MoveLearnMethodDraft =
    MoveLearnMethodDraft(
        code = code.trim().uppercase(Locale.ROOT),
        name = name.trim(),
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        sortingOrder = sortingOrder,
        enabled = enabled,
    )