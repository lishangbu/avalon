package io.github.lishangbu.avalon.catalog.domain


/**
 * 物种基础种族值。
 *
 * 第一阶段先把 battle 与 progression 都稳定依赖的六项基础数值收进 Catalog，
 * 避免各个上下文各自维护一份近似定义。
 *
 * @property hp 基础生命值。
 * @property attack 基础攻击。
 * @property defense 基础防御。
 * @property specialAttack 基础特攻。
 * @property specialDefense 基础特防。
 * @property speed 基础速度。
 */
data class SpeciesBaseStats(
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val specialAttack: Int,
    val specialDefense: Int,
    val speed: Int,
)

/**
 * 用于引用物种定义的轻量摘要。
 *
 * 该模型主要出现在进化定义等跨概念关联里，避免每次都展开完整物种明细。
 *
 * @property id 物种定义标识。
 * @property code 物种业务编码。
 * @property dexNumber 图鉴号。
 * @property name 物种展示名称。
 */
data class SpeciesSummary(
    val id: SpeciesId,
    val code: String,
    val dexNumber: Int,
    val name: String,
)

/**
 * 物种定义的领域视图。
 *
 * 第一阶段只维护图鉴号、主副属性和六维基础种族值这类稳定定义事实。
 * 成长率、进化链、可学习技能等更复杂的配套规则，会继续拆成独立切片维护。
 *
 * @property id 物种定义标识。
 * @property code 稳定业务编码，供跨上下文和管理端引用。
 * @property dexNumber 图鉴号。
 * @property name 物种展示名称。
 * @property description 物种说明，可为空。
 * @property primaryType 主属性摘要。
 * @property secondaryType 副属性摘要；单属性物种时为空。
 * @property growthRate 成长率摘要；尚未补齐历史数据时可为空。
 * @property baseStats 六维基础种族值。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前物种是否启用。
 * @property version 乐观锁版本号。
 */
data class Species(
    val id: SpeciesId,
    val code: String,
    val dexNumber: Int,
    val name: String,
    val description: String?,
    val primaryType: TypeDefinitionSummary,
    val secondaryType: TypeDefinitionSummary?,
    val growthRate: GrowthRateSummary?,
    val baseStats: SpeciesBaseStats,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 创建或更新物种时使用的输入草稿。
 *
 * @property code 物种业务编码。
 * @property dexNumber 图鉴号。
 * @property name 物种展示名称。
 * @property description 物种说明，可为空。
 * @property primaryTypeId 主属性标识。
 * @property secondaryTypeId 副属性标识；单属性物种时为空。
 * @property growthRateId 成长率标识；为兼容历史数据迁移当前允许为空。
 * @property baseStats 六维基础种族值。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前物种是否启用。
 */
data class SpeciesDraft(
    val code: String,
    val dexNumber: Int,
    val name: String,
    val description: String?,
    val primaryTypeId: TypeDefinitionId,
    val secondaryTypeId: TypeDefinitionId?,
    val growthRateId: GrowthRateId?,
    val baseStats: SpeciesBaseStats,
    val sortingOrder: Int,
    val enabled: Boolean,
)