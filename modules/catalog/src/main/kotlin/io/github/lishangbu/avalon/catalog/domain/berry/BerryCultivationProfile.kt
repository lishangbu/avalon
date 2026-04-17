package io.github.lishangbu.avalon.catalog.domain.berry

/**
 * 树果种植与收获资料。
 */
data class BerryCultivationProfile(
    val berryId: BerryDefinitionId,
    val growthHoursMin: Int?,
    val growthHoursMax: Int?,
    val yieldMin: Int?,
    val yieldMax: Int?,
    val cultivationSummary: String?,
    val version: Long,
)

data class BerryCultivationProfileDraft(
    val growthHoursMin: Int?,
    val growthHoursMax: Int?,
    val yieldMin: Int?,
    val yieldMax: Int?,
    val cultivationSummary: String?,
)
