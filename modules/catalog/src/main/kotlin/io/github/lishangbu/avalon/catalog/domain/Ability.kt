package io.github.lishangbu.avalon.catalog.domain


/**
 * 特性定义的领域视图。
 *
 * 第一阶段先把特性的稳定定义事实收进 Catalog，
 * 让 battle、progression 和内容维护端先共享同一份特性名称与说明。
 * 物种可拥有的特性集合、隐藏特性槽位等关联规则，后续再拆成独立切片。
 *
 * @property id 特性定义标识。
 * @property code 稳定业务编码，供跨上下文和管理端引用。
 * @property name 特性展示名称。
 * @property description 特性说明，可为空。
 * @property icon 前端展示时使用的图标标识，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前特性是否启用。
 * @property version 乐观锁版本号。
 */
data class Ability(
    val id: AbilityId,
    val code: String,
    val name: String,
    val description: String?,
    val icon: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 用于引用特性定义的轻量摘要。
 *
 * 该模型主要出现在物种特性关联等跨概念关系里，避免每次都展开完整特性明细。
 *
 * @property id 特性定义标识。
 * @property code 特性业务编码。
 * @property name 特性展示名称。
 */
data class AbilitySummary(
    val id: AbilityId,
    val code: String,
    val name: String,
)

/**
 * 创建或更新特性定义时使用的输入草稿。
 *
 * @property code 特性业务编码。
 * @property name 特性展示名称。
 * @property description 特性说明，可为空。
 * @property icon 图标标识，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前特性是否启用。
 */
data class AbilityDraft(
    val code: String,
    val name: String,
    val description: String?,
    val icon: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
)