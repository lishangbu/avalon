package io.github.lishangbu.avalon.catalog.interfaces.http.species

import io.github.lishangbu.avalon.catalog.domain.SpeciesAbility
import io.github.lishangbu.avalon.catalog.interfaces.http.reference.AbilitySummaryResponse
import io.github.lishangbu.avalon.catalog.interfaces.http.reference.toResponse
import java.util.UUID

/**
 * 物种特性关联响应。
 *
 * @property id 物种特性关联主键。
 * @property species 关联的物种摘要。
 * @property ability 关联的特性摘要。
 * @property slotCode 特性槽位业务码。
 * @property sortingOrder 同一物种下的展示顺序。
 * @property enabled 当前关联是否启用。
 * @property version 乐观锁版本号。
 */
data class SpeciesAbilityResponse(
    val id: UUID,
    val species: SpeciesSummaryResponse,
    val ability: AbilitySummaryResponse,
    val slotCode: String,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 把物种特性关联转换为接口响应。
 *
 * @return 可直接返回给调用方的物种特性关联明细。
 */
fun SpeciesAbility.toResponse(): SpeciesAbilityResponse =
    SpeciesAbilityResponse(
        id = id.value,
        species = species.toResponse(),
        ability = ability.toResponse(),
        slotCode = slotCode.name,
        sortingOrder = sortingOrder,
        enabled = enabled,
        version = version,
    )

