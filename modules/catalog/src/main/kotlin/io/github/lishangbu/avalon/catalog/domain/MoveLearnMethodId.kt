package io.github.lishangbu.avalon.catalog.domain

import java.util.UUID

/**
 * 招式学习方法标识。
 *
 * 使用独立类型而不是裸 `Long`，是为了在 Catalog 域内明确区分招式学习方法与其他参考数据，
 * 避免在应用服务、仓储和接口映射里误传主键值。
 *
 * @property value 招式学习方法主键值。
 */
@JvmInline
value class MoveLearnMethodId(
    val value: UUID,
)

