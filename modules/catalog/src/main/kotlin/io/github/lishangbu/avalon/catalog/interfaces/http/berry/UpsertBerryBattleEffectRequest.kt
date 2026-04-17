package io.github.lishangbu.avalon.catalog.interfaces.http.berry

import io.github.lishangbu.avalon.catalog.domain.berry.BerryBattleEffectDraft
import jakarta.validation.constraints.Positive

data class UpsertBerryBattleEffectRequest(
    val holdEffectSummary: String? = null,
    val directUseEffectSummary: String? = null,
    @field:Positive
    val flingPower: Int? = null,
    val flingEffectSummary: String? = null,
    val pluckEffectSummary: String? = null,
    val bugBiteEffectSummary: String? = null,
)

fun UpsertBerryBattleEffectRequest.toDraft(): BerryBattleEffectDraft =
    BerryBattleEffectDraft(
        holdEffectSummary = holdEffectSummary?.trim()?.takeIf { it.isNotEmpty() },
        directUseEffectSummary = directUseEffectSummary?.trim()?.takeIf { it.isNotEmpty() },
        flingPower = flingPower,
        flingEffectSummary = flingEffectSummary?.trim()?.takeIf { it.isNotEmpty() },
        pluckEffectSummary = pluckEffectSummary?.trim()?.takeIf { it.isNotEmpty() },
        bugBiteEffectSummary = bugBiteEffectSummary?.trim()?.takeIf { it.isNotEmpty() },
    )
