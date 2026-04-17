package io.github.lishangbu.avalon.catalog.interfaces.http.type

import io.github.lishangbu.avalon.catalog.domain.TypeEffectiveness
import java.math.BigDecimal

/**
 * 属性矩阵中的单个克制关系响应。
 *
 * @property attackingType 攻击方属性摘要。
 * @property defendingType 防守方属性摘要。
 * @property multiplier 攻击方对防守方的倍率。
 */
data class TypeChartEntryResponse(
    val attackingType: TypeDefinitionSummaryResponse,
    val defendingType: TypeDefinitionSummaryResponse,
    val multiplier: BigDecimal,
)

/**
 * 将属性克制关系转换为矩阵条目响应。
 */
fun TypeEffectiveness.toChartEntryResponse(): TypeChartEntryResponse =
    TypeChartEntryResponse(
        attackingType = attackingType.toResponse(),
        defendingType = defendingType.toResponse(),
        multiplier = multiplier,
    )
