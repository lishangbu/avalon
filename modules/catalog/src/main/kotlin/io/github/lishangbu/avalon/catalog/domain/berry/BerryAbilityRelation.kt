package io.github.lishangbu.avalon.catalog.domain.berry

/**
 * 树果与特性的百科关联。
 */
data class BerryAbilityRelation(
    val id: BerryAbilityRelationId,
    val berryId: BerryDefinitionId,
    val abilityCode: String,
    val abilityName: String,
    val relationKind: String,
    val note: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

data class BerryAbilityRelationDraft(
    val abilityCode: String,
    val abilityName: String,
    val relationKind: String,
    val note: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
)
