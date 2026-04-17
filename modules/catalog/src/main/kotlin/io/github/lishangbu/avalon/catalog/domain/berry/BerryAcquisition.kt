package io.github.lishangbu.avalon.catalog.domain.berry

/**
 * 树果获取方式。
 */
data class BerryAcquisition(
    val id: BerryAcquisitionId,
    val berryId: BerryDefinitionId,
    val sourceType: String,
    val conditionNote: String,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

data class BerryAcquisitionDraft(
    val sourceType: String,
    val conditionNote: String,
    val sortingOrder: Int,
    val enabled: Boolean,
)
