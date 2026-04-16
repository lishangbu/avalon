package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.domain.TypeEffectiveness
import java.math.BigDecimal
import java.util.UUID

/**
 * 类型克制关系响应。
 *
 * @property id 关系主键。
 * @property attackingType 攻击方类型摘要。
 * @property defendingType 防守方类型摘要。
 * @property multiplier 攻击方对防守方的倍率。
 * @property version 乐观锁版本号。
 */
data class TypeEffectivenessResponse(
    val id: UUID,
    val attackingType: TypeDefinitionSummaryResponse,
    val defendingType: TypeDefinitionSummaryResponse,
    val multiplier: BigDecimal,
    val version: Long,
)

/**
 * 将属性克制关系聚合转换为 HTTP 响应。
 *
 * @return 包含攻击方、防守方和倍率的响应对象。
 */
fun TypeEffectiveness.toResponse(): TypeEffectivenessResponse =
    TypeEffectivenessResponse(
        id = id.value,
        attackingType = attackingType.toResponse(),
        defendingType = defendingType.toResponse(),
        multiplier = multiplier,
        version = version,
    )

