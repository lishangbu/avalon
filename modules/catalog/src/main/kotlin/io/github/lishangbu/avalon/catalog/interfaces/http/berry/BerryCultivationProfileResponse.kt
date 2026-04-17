package io.github.lishangbu.avalon.catalog.interfaces.http.berry

import io.github.lishangbu.avalon.catalog.domain.berry.BerryCultivationProfile

data class BerryCultivationProfileResponse(
    val growthHoursMin: Int?,
    val growthHoursMax: Int?,
    val yieldMin: Int?,
    val yieldMax: Int?,
    val cultivationSummary: String?,
    val version: Long,
)

fun BerryCultivationProfile.toResponse(): BerryCultivationProfileResponse =
    BerryCultivationProfileResponse(
        growthHoursMin = growthHoursMin,
        growthHoursMax = growthHoursMax,
        yieldMin = yieldMin,
        yieldMax = yieldMax,
        cultivationSummary = cultivationSummary,
        version = version,
    )
