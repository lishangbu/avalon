package io.github.lishangbu.avalon.catalog.interfaces.http.berry

import io.github.lishangbu.avalon.catalog.domain.berry.BerryAcquisition
import java.util.UUID

data class BerryAcquisitionResponse(
    val id: UUID,
    val sourceType: String,
    val conditionNote: String,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

fun BerryAcquisition.toResponse(): BerryAcquisitionResponse =
    BerryAcquisitionResponse(
        id = id.value,
        sourceType = sourceType,
        conditionNote = conditionNote,
        sortingOrder = sortingOrder,
        enabled = enabled,
        version = version,
    )
