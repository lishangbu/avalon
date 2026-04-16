package io.github.lishangbu.avalon.catalog.interfaces.http.species

import io.github.lishangbu.avalon.catalog.domain.SpeciesEvolution
import java.util.UUID

/**
 * 物种进化定义响应。
 *
 * @property id 进化定义主键。
 * @property fromSpecies 进化起点物种摘要。
 * @property toSpecies 进化终点物种摘要。
 * @property triggerCode 进化触发方式业务码。
 * @property minLevel 等级进化时的最低等级；非等级进化时可为空。
 * @property description 进化说明，可为空。
 * @property sortingOrder 同一物种下的展示顺序。
 * @property enabled 当前进化定义是否启用。
 * @property version 乐观锁版本号。
 */
data class SpeciesEvolutionResponse(
    val id: UUID,
    val fromSpecies: SpeciesSummaryResponse,
    val toSpecies: SpeciesSummaryResponse,
    val triggerCode: String,
    val minLevel: Int?,
    val description: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 把物种进化定义转换为接口响应。
 *
 * @return 可直接返回给调用方的物种进化明细。
 */
fun SpeciesEvolution.toResponse(): SpeciesEvolutionResponse =
    SpeciesEvolutionResponse(
        id = id.value,
        fromSpecies = fromSpecies.toResponse(),
        toSpecies = toSpecies.toResponse(),
        triggerCode = triggerCode.name,
        minLevel = minLevel,
        description = description,
        sortingOrder = sortingOrder,
        enabled = enabled,
        version = version,
    )
