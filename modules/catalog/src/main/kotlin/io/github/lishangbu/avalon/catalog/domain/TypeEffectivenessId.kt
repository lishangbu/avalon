package io.github.lishangbu.avalon.catalog.domain

import java.util.UUID

/**
 * 属性克制关系标识。
 *
 * @property value 属性克制关系主键值。
 */
@JvmInline
value class TypeEffectivenessId(
    val value: UUID,
)

