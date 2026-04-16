package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.domain.CatalogBadRequest
import io.github.lishangbu.avalon.catalog.domain.NatureDraft
import io.github.lishangbu.avalon.catalog.domain.NatureModifierStatCode
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

/**
 * 创建或更新性格定义时使用的请求体。
 *
 * @property code 性格业务编码；映射到领域草稿时会统一转成大写。
 * @property name 性格展示名称。
 * @property description 性格说明，可为空。
 * @property increasedStatCode 被该性格正向修正的数值编码；中性性格时可为空。
 * @property decreasedStatCode 被该性格负向修正的数值编码；中性性格时可为空。
 * @property sortingOrder 列表展示顺序，值越小越靠前。
 * @property enabled 当前性格是否启用。
 */
data class UpsertNatureRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val code: String,
    @field:NotBlank
    @field:Size(max = 128)
    val name: String,
    @field:Size(max = 1000)
    val description: String? = null,
    @field:Size(max = 32)
    val increasedStatCode: String? = null,
    @field:Size(max = 32)
    val decreasedStatCode: String? = null,
    val sortingOrder: Int = 0,
    val enabled: Boolean = true,
)

/**
 * 把性格请求转换为领域草稿。
 *
 * 映射时会统一完成业务 code 标准化，并校验增减属性必须成对出现，
 * 且不能指向同一个数值。
 *
 * @return 可直接交给应用服务写入的性格草稿。
 * @throws IllegalArgumentException 当增减属性不成对、取值非法或重复时抛出。
 */
fun UpsertNatureRequest.toDraft(): NatureDraft {
    val increasedStat = increasedStatCode.toNatureModifierStatCode("increasedStatCode")
    val decreasedStat = decreasedStatCode.toNatureModifierStatCode("decreasedStatCode")

    if ((increasedStat == null) != (decreasedStat == null)) {
        throw CatalogBadRequest("increasedStatCode and decreasedStatCode must both be provided or omitted together.")
    }
    if (increasedStat != null && increasedStat == decreasedStat) {
        throw CatalogBadRequest("increasedStatCode and decreasedStatCode must not be the same.")
    }

    return NatureDraft(
        code = code.trim().uppercase(Locale.ROOT),
        name = name.trim(),
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        increasedStatCode = increasedStat,
        decreasedStatCode = decreasedStat,
        sortingOrder = sortingOrder,
        enabled = enabled,
    )
}

private fun String?.toNatureModifierStatCode(fieldName: String): NatureModifierStatCode? {
    val normalized = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return try {
        NatureModifierStatCode.valueOf(normalized.uppercase(Locale.ROOT))
    } catch (_: IllegalArgumentException) {
        val supportedCodes = NatureModifierStatCode.values().joinToString(", ") { it.name }
        throw CatalogBadRequest("$fieldName must be one of: $supportedCodes.")
    }
}