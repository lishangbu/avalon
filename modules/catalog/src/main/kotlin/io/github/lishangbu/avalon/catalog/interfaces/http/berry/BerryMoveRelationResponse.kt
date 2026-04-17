package io.github.lishangbu.avalon.catalog.interfaces.http.berry

import io.github.lishangbu.avalon.catalog.domain.berry.BerryMoveRelation
import java.util.UUID

data class BerryMoveRelationResponse(
    val id: UUID,
    val moveCode: String,
    val moveName: String,
    val relationKind: String,
    val note: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

fun BerryMoveRelation.toResponse(): BerryMoveRelationResponse =
    BerryMoveRelationResponse(
        id = id.value,
        moveCode = moveCode,
        moveName = moveName,
        relationKind = relationKind,
        note = note,
        sortingOrder = sortingOrder,
        enabled = enabled,
        version = version,
    )
