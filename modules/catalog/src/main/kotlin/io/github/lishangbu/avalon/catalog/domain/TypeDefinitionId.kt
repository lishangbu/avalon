package io.github.lishangbu.avalon.catalog.domain

import java.util.UUID

/**
 * 属性定义标识。
 *
 * 使用独立类型而不是裸 `Long`，是为了在 Catalog 域内明确区分
 * 类型定义和其他实体标识，减少误传参风险。
 *
 * @property value 属性定义主键值。
 */
@JvmInline
value class TypeDefinitionId(
    val value: UUID,
)

