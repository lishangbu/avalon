package io.github.lishangbu.avalon.catalog.domain

import java.util.UUID

/**
 * 性格定义标识。
 *
 * 使用独立类型而不是裸 `Long`，是为了在 Catalog 域内把性格和其他参考数据的
 * 主键值隔离开，避免在应用服务和仓储层误传参。
 *
 * @property value 性格定义主键值。
 */
@JvmInline
value class NatureId(
    val value: UUID,
)

