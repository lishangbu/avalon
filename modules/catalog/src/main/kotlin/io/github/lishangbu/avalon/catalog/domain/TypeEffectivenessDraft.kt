package io.github.lishangbu.avalon.catalog.domain

import java.math.BigDecimal

/**
 * 创建或更新属性克制关系时使用的输入草稿。
 *
 * @property attackingTypeId 攻击方类型标识。
 * @property defendingTypeId 防守方类型标识。
 * @property multiplier 攻击方对防守方的倍率。
 */
data class TypeEffectivenessDraft(
    val attackingTypeId: TypeDefinitionId,
    val defendingTypeId: TypeDefinitionId,
    val multiplier: BigDecimal,
)