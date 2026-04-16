package io.github.lishangbu.avalon.catalog.domain


/**
 * 物种进化定义的领域视图。
 *
 * 第一阶段先把“从哪个物种进化到哪个物种、通过什么方式触发”的边定义收进 Catalog，
 * 让 battle 和 progression 至少能共享一份稳定的进化关系事实。
 * 更复杂的整链编排、特殊条件和多阶段脚本，后续再拆成更完整的进化链切片。
 *
 * @property id 进化定义标识。
 * @property fromSpecies 进化起点物种摘要。
 * @property toSpecies 进化终点物种摘要。
 * @property triggerCode 进化触发方式。
 * @property minLevel 等级进化时的最低等级；非等级进化时可为空。
 * @property description 进化说明，可为空。
 * @property sortingOrder 同一物种下的展示顺序。
 * @property enabled 当前进化定义是否启用。
 * @property version 乐观锁版本号。
 */
data class SpeciesEvolution(
    val id: SpeciesEvolutionId,
    val fromSpecies: SpeciesSummary,
    val toSpecies: SpeciesSummary,
    val triggerCode: EvolutionTriggerCode,
    val minLevel: Int?,
    val description: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 创建或更新物种进化时使用的输入草稿。
 *
 * @property fromSpeciesId 进化起点物种标识。
 * @property toSpeciesId 进化终点物种标识。
 * @property triggerCode 进化触发方式。
 * @property minLevel 等级进化时的最低等级；非等级进化时可为空。
 * @property description 进化说明，可为空。
 * @property sortingOrder 同一物种下的展示顺序。
 * @property enabled 当前进化定义是否启用。
 */
data class SpeciesEvolutionDraft(
    val fromSpeciesId: SpeciesId,
    val toSpeciesId: SpeciesId,
    val triggerCode: EvolutionTriggerCode,
    val minLevel: Int?,
    val description: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
)