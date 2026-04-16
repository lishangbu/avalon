package io.github.lishangbu.avalon.catalog.domain

/**
 * 创建或更新属性定义时使用的输入草稿。
 *
 * @property code 类型业务编码。
 * @property name 类型展示名称。
 * @property description 类型说明，可为空。
 * @property icon 图标标识，可为空。
 * @property sortingOrder 列表展示顺序。
 * @property enabled 当前类型是否启用。
 */
data class TypeDefinitionDraft(
    val code: String,
    val name: String,
    val description: String?,
    val icon: String?,
    val sortingOrder: Int,
    val enabled: Boolean,
)