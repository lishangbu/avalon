package io.github.lishangbu.avalon.catalog.domain

import java.util.UUID

/**
 * 物种进化定义标识。
 *
 * 使用独立类型而不是裸 `Long`，是为了在 Catalog 域内明确区分进化定义与其他参考数据，
 * 避免在应用服务、仓储和接口映射里误传主键值。
 *
 * @property value 物种进化定义主键值。
 */
@JvmInline
value class SpeciesEvolutionId(
    val value: UUID,
)

