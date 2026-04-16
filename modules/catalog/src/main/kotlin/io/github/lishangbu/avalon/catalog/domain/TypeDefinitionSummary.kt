package io.github.lishangbu.avalon.catalog.domain

/**
 * 用于引用属性定义的轻量摘要。
 *
 * 该模型主要出现在类型克制关系里，避免每次都展开完整类型定义。
 *
 * @property id 类型定义标识。
 * @property code 类型业务编码。
 * @property name 类型展示名称。
 */
data class TypeDefinitionSummary(
    val id: TypeDefinitionId,
    val code: String,
    val name: String,
)