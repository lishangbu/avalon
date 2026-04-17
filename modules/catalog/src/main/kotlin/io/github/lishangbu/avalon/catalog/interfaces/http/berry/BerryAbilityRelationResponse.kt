package io.github.lishangbu.avalon.catalog.interfaces.http.berry

import io.github.lishangbu.avalon.catalog.domain.berry.BerryAbilityRelation
import java.util.UUID

data class BerryAbilityRelationResponse(
    val id: UUID,
    val abilityCode: String,
    val abilityName: String,
    val relationKind: String,
    val note: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

fun BerryAbilityRelation.toResponse(): BerryAbilityRelationResponse =
    BerryAbilityRelationResponse(
        id = id.value,
        abilityCode = abilityCode,
        abilityName = abilityName,
        relationKind = relationKind,
        note = note,
        sortingOrder = sortingOrder,
        enabled = enabled,
        version = version,
    )
