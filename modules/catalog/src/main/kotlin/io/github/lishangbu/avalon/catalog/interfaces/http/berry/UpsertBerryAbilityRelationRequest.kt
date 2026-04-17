package io.github.lishangbu.avalon.catalog.interfaces.http.berry

import io.github.lishangbu.avalon.catalog.domain.CatalogBadRequest
import io.github.lishangbu.avalon.catalog.domain.berry.BerryAbilityRelationDraft
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.Locale

data class UpsertBerryAbilityRelationRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val abilityCode: String,
    @field:NotBlank
    @field:Size(max = 128)
    val abilityName: String,
    @field:NotBlank
    @field:Size(max = 32)
    val relationKind: String,
    @field:Size(max = 1000)
    val note: String? = null,
    val sortingOrder: Int = 0,
    val enabled: Boolean = true,
)

fun UpsertBerryAbilityRelationRequest.toDraft(): BerryAbilityRelationDraft {
    val kind = relationKind.trim().uppercase(Locale.ROOT)
    if (kind !in ALLOWED_BERRY_ABILITY_RELATION_KINDS) {
        throw CatalogBadRequest("relationKind must be one of: ${ALLOWED_BERRY_ABILITY_RELATION_KINDS.joinToString()}.")
    }
    return BerryAbilityRelationDraft(
        abilityCode = abilityCode.trim().uppercase(Locale.ROOT),
        abilityName = abilityName.trim(),
        relationKind = kind,
        note = note?.trim()?.takeIf { it.isNotEmpty() },
        sortingOrder = sortingOrder,
        enabled = enabled,
    )
}

private val ALLOWED_BERRY_ABILITY_RELATION_KINDS = setOf("BATTLE_INTERACTION", "CULTIVATION_INTERACTION", "OTHER")
