package io.github.lishangbu.avalon.catalog.domain


/**
 * 属性定义的领域视图。
 *
 * @property id 类型定义标识。
 * @property code 稳定业务编码，供跨上下文和管理端引用。
 * @property name 类型展示名称。
 * @property description 类型说明，可为空。
 * @property icon 前端展示时使用的图标标识，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前类型是否启用。
 * @property version 乐观锁版本号。
 */
data class TypeDefinition(
    val id: TypeDefinitionId,
    val code: String,
    val name: String,
    val description: String?,
    val icon: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
    val version: Long,
)