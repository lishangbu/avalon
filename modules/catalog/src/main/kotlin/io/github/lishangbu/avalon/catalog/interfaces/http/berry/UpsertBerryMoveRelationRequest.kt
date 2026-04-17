package io.github.lishangbu.avalon.catalog.interfaces.http.berry

import io.github.lishangbu.avalon.catalog.domain.CatalogBadRequest
import io.github.lishangbu.avalon.catalog.domain.berry.BerryMoveRelationDraft
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.Locale

data class UpsertBerryMoveRelationRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val moveCode: String,
    @field:NotBlank
    @field:Size(max = 128)
    val moveName: String,
    @field:NotBlank
    @field:Size(max = 32)
    val relationKind: String,
    @field:Size(max = 1000)
    val note: String? = null,
    val sortingOrder: Int = 0,
    val enabled: Boolean = true,
)

fun UpsertBerryMoveRelationRequest.toDraft(): BerryMoveRelationDraft {
    val kind = relationKind.trim().uppercase(Locale.ROOT)
    if (kind !in ALLOWED_BERRY_MOVE_RELATION_KINDS) {
        throw CatalogBadRequest("relationKind must be one of: ${ALLOWED_BERRY_MOVE_RELATION_KINDS.joinToString()}.")
    }
    return BerryMoveRelationDraft(
        moveCode = moveCode.trim().uppercase(Locale.ROOT),
        moveName = moveName.trim(),
        relationKind = kind,
        note = note?.trim()?.takeIf { it.isNotEmpty() },
        sortingOrder = sortingOrder,
        enabled = enabled,
    )
}

private val ALLOWED_BERRY_MOVE_RELATION_KINDS = setOf("FLING", "NATURAL_GIFT", "PLUCK", "BUG_BITE", "CUSTOM")
