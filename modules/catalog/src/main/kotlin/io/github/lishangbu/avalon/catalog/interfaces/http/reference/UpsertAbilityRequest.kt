package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.domain.AbilityDraft
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

/**
 * 创建或更新特性定义时使用的请求体。
 *
 * @property code 特性业务编码；映射到领域草稿时会统一转成大写。
 * @property name 特性展示名称。
 * @property description 特性说明，可为空。
 * @property icon 前端展示时使用的图标标识，可为空。
 * @property sortingOrder 列表展示顺序，值越小越靠前。
 * @property enabled 当前特性是否启用。
 */
data class UpsertAbilityRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val code: String,
    @field:NotBlank
    @field:Size(max = 128)
    val name: String,
    @field:Size(max = 1000)
    val description: String? = null,
    @field:Size(max = 255)
    val icon: String? = null,
    val sortingOrder: Int = 0,
    val enabled: Boolean = true,
)

/**
 * 把特性请求转换为领域草稿。
 *
 * @return 可直接交给应用服务写入的特性草稿。
 */
fun UpsertAbilityRequest.toDraft(): AbilityDraft =
    AbilityDraft(
        code = code.trim().uppercase(Locale.ROOT),
        name = name.trim(),
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        icon = icon?.trim()?.takeIf { it.isNotEmpty() },
        sortingOrder = sortingOrder,
        enabled = enabled,
    )