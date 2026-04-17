package io.github.lishangbu.avalon.catalog.interfaces.http.berry

import io.github.lishangbu.avalon.catalog.domain.berry.BerryBattleEffect

data class BerryBattleEffectResponse(
    val holdEffectSummary: String?,
    val directUseEffectSummary: String?,
    val flingPower: Int?,
    val flingEffectSummary: String?,
    val pluckEffectSummary: String?,
    val bugBiteEffectSummary: String?,
    val version: Long,
)

fun BerryBattleEffect.toResponse(): BerryBattleEffectResponse =
    BerryBattleEffectResponse(
        holdEffectSummary = holdEffectSummary,
        directUseEffectSummary = directUseEffectSummary,
        flingPower = flingPower,
        flingEffectSummary = flingEffectSummary,
        pluckEffectSummary = pluckEffectSummary,
        bugBiteEffectSummary = bugBiteEffectSummary,
        version = version,
    )
