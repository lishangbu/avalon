package io.github.lishangbu.avalon.catalog.interfaces.http.move

import io.github.lishangbu.avalon.catalog.domain.MoveTargetDraft
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

/**
 * 创建或更新招式目标时使用的请求体。
 *
 * @property code 招式目标业务编码，映射到领域草稿时会统一转成大写。
 * @property name 招式目标展示名称。
 * @property description 招式目标说明，可为空。
 * @property sortingOrder 列表展示顺序，数值越小越靠前。
 * @property enabled 当前招式目标是否启用。
 */
data class UpsertMoveTargetRequest(
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
 * 将招式目标请求体转换为领域草稿。
 *
 * @return 可直接交给应用服务写入的招式目标草稿。
 */
fun UpsertMoveTargetRequest.toDraft(): MoveTargetDraft =
    MoveTargetDraft(
        code = code.trim().uppercase(Locale.ROOT),
        name = name.trim(),
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        sortingOrder = sortingOrder,
        enabled = enabled,
    )