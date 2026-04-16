package io.github.lishangbu.avalon.catalog.domain


/**
 * 性格定义的领域视图。
 *
 * 性格本身是 `Catalog` 中可被多个上下文共享读取的定义事实。
 * 第一阶段只维护它的稳定业务编码、展示属性，以及对数值的增减方向；
 * 战斗时如何把这些定义转成运行时数值修正，仍由消费方在本域内转换。
 *
 * @property id 性格定义标识。
 * @property code 稳定业务编码，供跨上下文和管理端引用。
 * @property name 性格展示名称。
 * @property description 性格说明，可为空。
 * @property increasedStatCode 被该性格正向修正的数值；中性性格时为空。
 * @property decreasedStatCode 被该性格负向修正的数值；中性性格时为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前性格是否启用。
 * @property version 乐观锁版本号。
 */
data class Nature(
    val id: NatureId,
    val code: String,
    val name: String,
    val description: String?,
    val increasedStatCode: NatureModifierStatCode?,
    val decreasedStatCode: NatureModifierStatCode?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 创建或更新性格时使用的输入草稿。
 *
 * @property code 性格业务编码。
 * @property name 性格展示名称。
 * @property description 性格说明，可为空。
 * @property increasedStatCode 正向修正的数值；中性性格时为空。
 * @property decreasedStatCode 负向修正的数值；中性性格时为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前性格是否启用。
 */
data class NatureDraft(
    val code: String,
    val name: String,
    val description: String?,
    val increasedStatCode: NatureModifierStatCode?,
    val decreasedStatCode: NatureModifierStatCode?,
    val sortingOrder: Int,
    val enabled: Boolean,
)