package io.github.lishangbu.avalon.identity.access.domain.iam

import java.util.UUID

/**
 * 角色聚合标识。
 *
 * @property value 角色主键值。
 */
@JvmInline
value class RoleId(
    val value: UUID,
)

