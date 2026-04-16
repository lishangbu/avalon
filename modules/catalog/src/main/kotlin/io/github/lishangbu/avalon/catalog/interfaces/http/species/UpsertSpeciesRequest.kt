package io.github.lishangbu.avalon.catalog.interfaces.http.species

import io.github.lishangbu.avalon.catalog.domain.CatalogBadRequest
import io.github.lishangbu.avalon.catalog.domain.GrowthRateId
import io.github.lishangbu.avalon.catalog.domain.SpeciesDraft
import io.github.lishangbu.avalon.catalog.domain.TypeDefinitionId
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import jakarta.validation.Valid
import java.util.*
import java.util.UUID

/**
 * 创建或更新物种定义时使用的请求体。
 *
 * @property code 物种业务编码；映射到领域草稿时会统一转成大写。
 * @property dexNumber 图鉴号，必须大于 0。
 * @property name 物种展示名称。
 * @property description 物种说明，可为空。
 * @property primaryTypeId 主属性主键。
 * @property secondaryTypeId 副属性主键；单属性物种时可为空。
 * @property growthRateId 成长率主键；历史数据补齐前当前允许为空。
 * @property baseStats 六维基础种族值。
 * @property sortingOrder 列表展示顺序，值越小越靠前。
 * @property enabled 当前物种是否启用。
 */
data class UpsertSpeciesRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val code: String,
    @field:Positive
    val dexNumber: Int,
    @field:NotBlank
    @field:Size(max = 128)
    val name: String,
    @field:Size(max = 1000)
    val description: String? = null,
    val primaryTypeId: UUID,
    val secondaryTypeId: UUID? = null,
    val growthRateId: UUID? = null,
    @field:Valid
    val baseStats: SpeciesBaseStatsRequest,
    val sortingOrder: Int = 0,
    val enabled: Boolean = true,
)

/**
 * 把物种请求转换为领域草稿。
 *
 * @return 可直接交给应用服务写入的物种草稿。
 * @throws IllegalArgumentException 当主副属性相同或输入不满足约束时抛出。
 */
fun UpsertSpeciesRequest.toDraft(): SpeciesDraft {
    val primaryType = TypeDefinitionId(primaryTypeId)
    val secondaryType = secondaryTypeId?.let(::TypeDefinitionId)
    if (secondaryType != null && secondaryType == primaryType) {
        throw CatalogBadRequest("secondaryTypeId must be different from primaryTypeId.")
    }

    return SpeciesDraft(
        code = code.trim().uppercase(Locale.ROOT),
        dexNumber = dexNumber,
        name = name.trim(),
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        primaryTypeId = primaryType,
        secondaryTypeId = secondaryType,
        growthRateId = growthRateId?.let(::GrowthRateId),
        baseStats = baseStats.toDomain(),
        sortingOrder = sortingOrder,
        enabled = enabled,
    )
}

