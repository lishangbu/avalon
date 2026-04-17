package io.github.lishangbu.avalon.catalog.domain.berry

/**
 * 树果与招式的百科关联。
 */
data class BerryMoveRelation(
    val id: BerryMoveRelationId,
    val berryId: BerryDefinitionId,
    val moveCode: String,
    val moveName: String,
    val relationKind: String,
    val note: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

data class BerryMoveRelationDraft(
    val moveCode: String,
    val moveName: String,
    val relationKind: String,
    val note: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
)
