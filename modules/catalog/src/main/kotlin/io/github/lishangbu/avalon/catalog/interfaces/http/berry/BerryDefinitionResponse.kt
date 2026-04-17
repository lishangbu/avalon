package io.github.lishangbu.avalon.catalog.interfaces.http.berry

import io.github.lishangbu.avalon.catalog.domain.TypeDefinitionSummary
import io.github.lishangbu.avalon.catalog.domain.berry.BerryDefinition
import io.github.lishangbu.avalon.catalog.interfaces.http.type.TypeDefinitionSummaryResponse
import io.github.lishangbu.avalon.catalog.interfaces.http.type.toResponse
import java.math.BigDecimal
import java.util.UUID

data class BerryDefinitionResponse(
    val id: UUID,
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
    val naturalGiftType: TypeDefinitionSummaryResponse?,
    val naturalGiftPower: Int?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

fun BerryDefinition.toResponse(): BerryDefinitionResponse =
    BerryDefinitionResponse(
        id = id.value,
        code = code,
        name = name,
        description = description,
        icon = icon,
        colorCode = colorCode,
        firmnessCode = firmnessCode,
        sizeCm = sizeCm,
        smoothness = smoothness,
        spicy = spicy,
        dry = dry,
        sweet = sweet,
        bitter = bitter,
        sour = sour,
        naturalGiftType = naturalGiftType?.toResponse(),
        naturalGiftPower = naturalGiftPower,
        sortingOrder = sortingOrder,
        enabled = enabled,
        version = version,
    )
