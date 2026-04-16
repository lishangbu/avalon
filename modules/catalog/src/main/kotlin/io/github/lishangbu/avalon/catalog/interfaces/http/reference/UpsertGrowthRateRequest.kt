package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.domain.CatalogBadRequest
import io.github.lishangbu.avalon.catalog.domain.GrowthRateDraft
import io.github.lishangbu.avalon.catalog.domain.GrowthRateFormulaCode
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

/**
 * 创建或更新成长率定义时使用的请求体。
 *
 * @property code 成长率业务编码；映射到领域草稿时会统一转成大写。
 * @property name 成长率展示名称。
 * @property formulaCode 经验公式业务码；映射时会统一转成大写。
 * @property description 成长率说明，可为空。
 * @property sortingOrder 列表展示顺序，值越小越靠前。
 * @property enabled 当前成长率是否启用。
 */
data class UpsertGrowthRateRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val code: String,
    @field:NotBlank
    @field:Size(max = 128)
    val name: String,
    @field:NotBlank
    @field:Size(max = 32)
    val formulaCode: String,
    @field:Size(max = 1000)
    val description: String? = null,
    val sortingOrder: Int = 0,
    val enabled: Boolean = true,
)

/**
 * 把成长率请求转换为领域草稿。
 *
 * @return 可直接交给应用服务写入的成长率草稿。
 * @throws IllegalArgumentException 当 formulaCode 不受支持时抛出。
 */
fun UpsertGrowthRateRequest.toDraft(): GrowthRateDraft =
    GrowthRateDraft(
        code = code.trim().uppercase(Locale.ROOT),
        name = name.trim(),
        formulaCode = formulaCode.toGrowthRateFormulaCode(),
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        sortingOrder = sortingOrder,
        enabled = enabled,
    )

private fun String.toGrowthRateFormulaCode(): GrowthRateFormulaCode {
    val normalized = trim().uppercase(Locale.ROOT)
    return try {
        GrowthRateFormulaCode.valueOf(normalized)
    } catch (_: IllegalArgumentException) {
        val supportedCodes = GrowthRateFormulaCode.values().joinToString(", ") { it.name }
        throw CatalogBadRequest("formulaCode must be one of: $supportedCodes.")
    }
}