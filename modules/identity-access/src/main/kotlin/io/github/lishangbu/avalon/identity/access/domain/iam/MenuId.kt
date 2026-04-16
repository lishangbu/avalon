package io.github.lishangbu.avalon.identity.access.domain.iam

import java.util.UUID

/**
 * 菜单聚合标识。
 *
 * @property value 菜单主键值。
 */
@JvmInline
value class MenuId(
    val value: UUID,
)

