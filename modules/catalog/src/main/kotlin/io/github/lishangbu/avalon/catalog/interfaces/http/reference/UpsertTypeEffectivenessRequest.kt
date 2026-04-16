package io.github.lishangbu.avalon.catalog.interfaces.http.reference

import io.github.lishangbu.avalon.catalog.domain.TypeDefinitionId
import io.github.lishangbu.avalon.catalog.domain.TypeEffectivenessDraft
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.UUID

/**
 * 创建或更新属性克制关系时使用的请求体。
 *
 * @property attackingTypeId 攻击方类型主键。
 * @property defendingTypeId 防守方类型主键。
 * @property multiplier 攻击方对防守方的倍率。
 */
data class UpsertTypeEffectivenessRequest(
    @field:NotNull
    val attackingTypeId: UUID,
    @field:NotNull
    val defendingTypeId: UUID,
    @field:NotNull
    @field:DecimalMin("0.0")
    @field:DecimalMax("4.0")
    @field:Digits(integer = 2, fraction = 2)
    val multiplier: BigDecimal,
)

/**
 * 将属性克制关系请求转换为领域草稿。
 *
 * @return 已完成标识封装和倍率标准化的领域草稿。
 */
fun UpsertTypeEffectivenessRequest.toDraft(): TypeEffectivenessDraft =
    TypeEffectivenessDraft(
        attackingTypeId = TypeDefinitionId(attackingTypeId),
        defendingTypeId = TypeDefinitionId(defendingTypeId),
        multiplier = multiplier.stripTrailingZeros(),
    )

