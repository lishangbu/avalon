package io.github.lishangbu.avalon.catalog.interfaces.http.berry

import io.github.lishangbu.avalon.catalog.domain.CatalogBadRequest
import io.github.lishangbu.avalon.catalog.domain.berry.BerryDefinitionDraft
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.Locale
import java.util.UUID

data class UpsertBerryDefinitionRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val code: String,
    @field:NotBlank
    @field:Size(max = 128)
    val name: String,
    @field:Size(max = 1000)
    val description: String? = null,
    @field:Size(max = 255)
    val icon: String? = null,
    @field:Size(max = 32)
    val colorCode: String? = null,
    @field:Size(max = 32)
    val firmnessCode: String? = null,
    val sizeCm: BigDecimal? = null,
    @field:Positive
    val smoothness: Int? = null,
    val spicy: Int = 0,
    val dry: Int = 0,
    val sweet: Int = 0,
    val bitter: Int = 0,
    val sour: Int = 0,
    val naturalGiftTypeId: UUID? = null,
    @field:Positive
    val naturalGiftPower: Int? = null,
    val sortingOrder: Int = 0,
    val enabled: Boolean = true,
)

fun UpsertBerryDefinitionRequest.toDraft(): BerryDefinitionDraft {
    val color = colorCode?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }
    val firmness = firmnessCode?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }
    if (color != null && color !in ALLOWED_COLOR_CODES) {
        throw CatalogBadRequest("colorCode must be one of: ${ALLOWED_COLOR_CODES.joinToString()}.")
    }
    if (firmness != null && firmness !in ALLOWED_FIRMNESS_CODES) {
        throw CatalogBadRequest("firmnessCode must be one of: ${ALLOWED_FIRMNESS_CODES.joinToString()}.")
    }
    return BerryDefinitionDraft(
        code = code.normalizeBerryCode(),
        name = name.trim(),
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        icon = icon?.trim()?.takeIf { it.isNotEmpty() },
        colorCode = color,
        firmnessCode = firmness,
        sizeCm = sizeCm,
        smoothness = smoothness,
        spicy = spicy,
        dry = dry,
        sweet = sweet,
        bitter = bitter,
        sour = sour,
        naturalGiftTypeId = naturalGiftTypeId,
        naturalGiftPower = naturalGiftPower,
        sortingOrder = sortingOrder,
        enabled = enabled,
    )
}

private fun String.normalizeBerryCode(): String =
    trim()
        .replace(Regex("[\\s-]+"), "_")
        .uppercase(Locale.ROOT)

private val ALLOWED_COLOR_CODES = setOf("RED", "BLUE", "YELLOW", "GREEN", "PINK", "PURPLE", "BROWN", "BLACK", "WHITE")
private val ALLOWED_FIRMNESS_CODES = setOf("VERY_SOFT", "SOFT", "HARD", "VERY_HARD", "SUPER_HARD")
