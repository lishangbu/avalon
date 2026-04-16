package io.github.lishangbu.avalon.catalog.domain


/**
 * 成长率定义的领域视图。
 *
 * 成长率本身是 `Catalog` 中可被多个上下文共享读取的规则事实。
 * 第一阶段只维护稳定业务编码、展示属性和经验公式码；
 * 具体如何根据公式码计算经验曲线，仍由消费方在本域内转换。
 *
 * @property id 成长率定义标识。
 * @property code 稳定业务编码，供跨上下文和管理端引用。
 * @property name 成长率展示名称。
 * @property formulaCode 经验公式码。
 * @property description 成长率说明，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前成长率是否启用。
 * @property version 乐观锁版本号。
 */
data class GrowthRate(
    val id: GrowthRateId,
    val code: String,
    val name: String,
    val formulaCode: GrowthRateFormulaCode,
    val description: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 用于引用成长率定义的轻量摘要。
 *
 * 该模型主要出现在物种明细里，避免每次都展开完整成长率定义。
 *
 * @property id 成长率定义标识。
 * @property code 成长率业务编码。
 * @property name 成长率展示名称。
 * @property formulaCode 经验公式码。
 */
data class GrowthRateSummary(
    val id: GrowthRateId,
    val code: String,
    val name: String,
    val formulaCode: GrowthRateFormulaCode,
)

/**
 * 创建或更新成长率时使用的输入草稿。
 *
 * @property code 成长率业务编码。
 * @property name 成长率展示名称。
 * @property formulaCode 经验公式码。
 * @property description 成长率说明，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前成长率是否启用。
 */
data class GrowthRateDraft(
    val code: String,
    val name: String,
    val formulaCode: GrowthRateFormulaCode,
    val description: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
)