package io.github.lishangbu.avalon.catalog.domain


/**
 * 道具定义的领域视图。
 *
 * 第一阶段只维护对多个上下文都稳定成立的道具元数据，
 * 例如业务编码、分类码、展示属性和堆叠上限。
 * 道具在战斗或成长流程里的具体行为，仍由消费方在本域内转换和解释。
 *
 * @property id 道具定义标识。
 * @property code 稳定业务编码，供跨上下文和管理端引用。
 * @property name 道具展示名称。
 * @property categoryCode 道具分类业务码，用于管理端分组和下游语义转换。
 * @property description 道具说明，可为空。
 * @property icon 前端展示时使用的图标标识，可为空。
 * @property maxStackSize 单格最大堆叠数量。
 * @property consumable 当前道具是否属于消耗型道具。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前道具是否启用。
 * @property version 乐观锁版本号。
 */
data class Item(
    val id: ItemId,
    val code: String,
    val name: String,
    val categoryCode: String,
    val description: String?,
    val icon: String?,
    val maxStackSize: Int,
    val consumable: Boolean,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)

/**
 * 创建或更新道具时使用的输入草稿。
 *
 * @property code 道具业务编码。
 * @property name 道具展示名称。
 * @property categoryCode 道具分类业务码。
 * @property description 道具说明，可为空。
 * @property icon 图标标识，可为空。
 * @property maxStackSize 单格最大堆叠数量。
 * @property consumable 当前道具是否属于消耗型道具。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前道具是否启用。
 */
data class ItemDraft(
    val code: String,
    val name: String,
    val categoryCode: String,
    val description: String?,
    val icon: String?,
    val maxStackSize: Int,
    val consumable: Boolean,
    val sortingOrder: Int,
    val enabled: Boolean,
)