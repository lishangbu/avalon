package io.github.lishangbu.avalon.catalog.domain

import java.math.BigDecimal

/**
 * 属性克制关系的领域视图。
 *
 * @property id 克制关系标识。
 * @property attackingType 攻击方类型摘要。
 * @property defendingType 防守方类型摘要。
 * @property multiplier 攻击方对防守方的倍率。
 * @property version 乐观锁版本号。
 */
data class TypeEffectiveness(
    val id: TypeEffectivenessId,
    val attackingType: TypeDefinitionSummary,
    val defendingType: TypeDefinitionSummary,
    val multiplier: BigDecimal,
    val version: Long,
)