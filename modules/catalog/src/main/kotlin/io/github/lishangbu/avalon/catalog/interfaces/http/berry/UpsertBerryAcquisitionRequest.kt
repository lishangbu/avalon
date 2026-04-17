package io.github.lishangbu.avalon.catalog.interfaces.http.berry

import io.github.lishangbu.avalon.catalog.domain.CatalogBadRequest
import io.github.lishangbu.avalon.catalog.domain.berry.BerryAcquisitionDraft
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.Locale

data class UpsertBerryAcquisitionRequest(
    @field:NotBlank
    @field:Size(max = 32)
    val sourceType: String,
    @field:NotBlank
    @field:Size(max = 1000)
    val conditionNote: String,
    val sortingOrder: Int = 0,
    val enabled: Boolean = true,
)

fun UpsertBerryAcquisitionRequest.toDraft(): BerryAcquisitionDraft {
    val source = sourceType.trim().uppercase(Locale.ROOT)
    if (source !in ALLOWED_SOURCE_TYPES) {
        throw CatalogBadRequest("sourceType must be one of: ${ALLOWED_SOURCE_TYPES.joinToString()}.")
    }
    return BerryAcquisitionDraft(
        sourceType = source,
        conditionNote = conditionNote.trim(),
        sortingOrder = sortingOrder,
        enabled = enabled,
    )
}

private val ALLOWED_SOURCE_TYPES = setOf(
    "BERRY_TREE", "BERRY_MASTER", "NATURAL_OBJECT", "FIELD_PICKUP", "PICKUP",
    "WILD_HELD_ITEM", "TRAINER_HELD_ITEM", "NPC_GIFT", "EVENT_REWARD", "EXCHANGE", "OTHER",
)
