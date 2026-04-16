package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.domain.TypeDefinitionDraft
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

/**
 * 创建或更新属性定义时使用的请求体。
 *
 * @property code 类型业务编码；映射到领域草稿时会统一转成大写。
 * @property name 类型展示名称。
 * @property description 类型说明，可为空。
 * @property icon 前端展示时使用的图标标识，可为空。
 * @property sortingOrder 列表展示顺序，值越小越靠前。
 * @property enabled 当前类型是否启用。
 */
data class UpsertTypeDefinitionRequest(
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
 * 将类型定义请求转换为领域草稿。
 *
 * @return 供应用服务直接写入的类型定义草稿。
 */
fun UpsertTypeDefinitionRequest.toDraft(): TypeDefinitionDraft =
    TypeDefinitionDraft(
        code = code.trim().uppercase(Locale.ROOT),
        name = name.trim(),
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        icon = icon?.trim()?.takeIf { it.isNotEmpty() },
        sortingOrder = sortingOrder,
        enabled = enabled,
    )