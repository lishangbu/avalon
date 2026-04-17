package io.github.lishangbu.avalon.catalog.domain.berry

import io.github.lishangbu.avalon.catalog.domain.TypeDefinitionSummary
import java.math.BigDecimal
import java.util.UUID

/**
 * 树果基础档案。
 *
 * 该聚合只描述树果本体静态事实，不承载对战、种植或获取方式等从属明细。
 */
data class BerryDefinition(
    val id: BerryDefinitionId,
    val code: String,
    val name: String,
    val description: String?,
    val icon: String?,
    val colorCode: String?,
    val firmnessCode: String?,
    val sizeCm: BigDecimal?,
    val smoothness: Int?,
    val spicy: Int,
    val dry: Int,
    val sweet: Int,
    val bitter: Int,
    val sour: Int,
    val naturalGiftType: TypeDefinitionSummary?,
    val naturalGiftPower: Int?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

data class BerryDefinitionDraft(
    val code: String,
    val name: String,
    val description: String?,
    val icon: String?,
    val colorCode: String?,
    val firmnessCode: String?,
    val sizeCm: BigDecimal?,
    val smoothness: Int?,
    val spicy: Int,
    val dry: Int,
    val sweet: Int,
    val bitter: Int,
    val sour: Int,
    val naturalGiftTypeId: UUID?,
    val naturalGiftPower: Int?,
    val sortingOrder: Int,
    val enabled: Boolean,
)
