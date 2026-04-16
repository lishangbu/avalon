package io.github.lishangbu.avalon.catalog.domain


/**
 * 物种特性关联的领域视图。
 *
 * 该切片负责表达“某个物种在哪个槽位上拥有哪个特性”这类稳定参考事实。
 * battle 和 progression 读取后仍需在本域内转换成自己的运行时规则模型，
 * Catalog 不直接承担运行时特性解析。
 *
 * @property id 物种特性关联标识。
 * @property species 关联的物种摘要。
 * @property ability 关联的特性摘要。
 * @property slotCode 特性槽位类型。
 * @property sortingOrder 同一物种下的展示顺序。
 * @property enabled 当前关联是否启用。
 * @property version 乐观锁版本号。
 */
data class SpeciesAbility(
    val id: SpeciesAbilityId,
    val species: SpeciesSummary,
    val ability: AbilitySummary,
    val slotCode: AbilitySlotCode,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 创建或更新物种特性关联时使用的输入草稿。
 *
 * @property speciesId 物种定义标识。
 * @property abilityId 特性定义标识。
 * @property slotCode 特性槽位类型。
 * @property sortingOrder 同一物种下的展示顺序。
 * @property enabled 当前关联是否启用。
 */
data class SpeciesAbilityDraft(
    val speciesId: SpeciesId,
    val abilityId: AbilityId,
    val slotCode: AbilitySlotCode,
    val sortingOrder: Int,
    val enabled: Boolean,
)