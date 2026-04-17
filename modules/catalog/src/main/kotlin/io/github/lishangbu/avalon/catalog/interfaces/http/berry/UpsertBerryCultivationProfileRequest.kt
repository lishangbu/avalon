package io.github.lishangbu.avalon.catalog.interfaces.http.berry

import io.github.lishangbu.avalon.catalog.domain.berry.BerryCultivationProfileDraft
import jakarta.validation.constraints.Positive

data class UpsertBerryCultivationProfileRequest(
    @field:Positive
    val growthHoursMin: Int? = null,
    @field:Positive
    val growthHoursMax: Int? = null,
    @field:Positive
    val yieldMin: Int? = null,
    @field:Positive
    val yieldMax: Int? = null,
    val cultivationSummary: String? = null,
)

fun UpsertBerryCultivationProfileRequest.toDraft(): BerryCultivationProfileDraft =
    BerryCultivationProfileDraft(
        growthHoursMin = growthHoursMin,
        growthHoursMax = growthHoursMax,
        yieldMin = yieldMin,
        yieldMax = yieldMax,
        cultivationSummary = cultivationSummary?.trim()?.takeIf { it.isNotEmpty() },
    )
