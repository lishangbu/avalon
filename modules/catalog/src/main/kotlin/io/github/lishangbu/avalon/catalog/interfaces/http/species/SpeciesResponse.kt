package io.github.lishangbu.avalon.catalog.interfaces.http.species

import io.github.lishangbu.avalon.catalog.domain.Species
import io.github.lishangbu.avalon.catalog.interfaces.http.reference.GrowthRateSummaryResponse
import io.github.lishangbu.avalon.catalog.interfaces.http.reference.toResponse
import io.github.lishangbu.avalon.catalog.interfaces.http.type.TypeDefinitionSummaryResponse
import io.github.lishangbu.avalon.catalog.interfaces.http.type.toResponse
import java.util.UUID

/**
 * 物种明细响应。
 *
 * @property id 物种主键。
 * @property code 物种业务编码。
 * @property dexNumber 图鉴号。
 * @property name 物种展示名称。
 * @property description 物种说明，可为空。
 * @property primaryType 主属性摘要。
 * @property secondaryType 副属性摘要；单属性物种时为空。
 * @property growthRate 成长率摘要；历史数据补齐前当前允许为空。
 * @property baseStats 六维基础种族值。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前物种是否启用。
 * @property version 乐观锁版本号。
 */
data class SpeciesResponse(
    val id: UUID,
    val code: String,
    val dexNumber: Int,
    val name: String,
    val description: String?,
    val primaryType: TypeDefinitionSummaryResponse,
    val secondaryType: TypeDefinitionSummaryResponse?,
    val growthRate: GrowthRateSummaryResponse?,
    val baseStats: SpeciesBaseStatsResponse,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 把物种定义转换为接口响应。
 *
 * @return 可直接返回给调用方的物种明细。
 */
fun Species.toResponse(): SpeciesResponse =
    SpeciesResponse(
        id = id.value,
        code = code,
        dexNumber = dexNumber,
        name = name,
        description = description,
        primaryType = primaryType.toResponse(),
        secondaryType = secondaryType?.toResponse(),
        growthRate = growthRate?.toResponse(),
        baseStats = baseStats.toResponse(),
        sortingOrder = sortingOrder,
        enabled = enabled,
        version = version,
    )

