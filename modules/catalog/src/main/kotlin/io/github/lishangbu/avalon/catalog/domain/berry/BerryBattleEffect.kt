package io.github.lishangbu.avalon.catalog.domain.berry

/**
 * 树果在对战中的百科化效果资料。
 */
data class BerryBattleEffect(
    val berryId: BerryDefinitionId,
    val holdEffectSummary: String?,
    val directUseEffectSummary: String?,
    val flingPower: Int?,
    val flingEffectSummary: String?,
    val pluckEffectSummary: String?,
    val bugBiteEffectSummary: String?,
    val version: Long,
)

data class BerryBattleEffectDraft(
    val holdEffectSummary: String?,
    val directUseEffectSummary: String?,
    val flingPower: Int?,
    val flingEffectSummary: String?,
    val pluckEffectSummary: String?,
    val bugBiteEffectSummary: String?,
)
