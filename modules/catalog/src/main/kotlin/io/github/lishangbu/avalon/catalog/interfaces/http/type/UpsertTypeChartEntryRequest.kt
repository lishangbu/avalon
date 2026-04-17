package io.github.lishangbu.avalon.catalog.interfaces.http.type

import io.github.lishangbu.avalon.catalog.domain.TypeDefinitionId
import io.github.lishangbu.avalon.catalog.domain.TypeEffectivenessDraft
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.UUID

/**
 * 属性矩阵中的单个写入条目。
 *
 * @property attackingTypeId 攻击方属性主键。
 * @property defendingTypeId 防守方属性主键。
 * @property multiplier 攻击方对防守方的倍率。
 */
data class UpsertTypeChartEntryRequest(
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
 * 将矩阵条目请求转换为领域草稿。
 */
fun UpsertTypeChartEntryRequest.toDraft(): TypeEffectivenessDraft =
    TypeEffectivenessDraft(
        attackingTypeId = TypeDefinitionId(attackingTypeId),
        defendingTypeId = TypeDefinitionId(defendingTypeId),
        multiplier = multiplier.stripTrailingZeros(),
    )
