package io.github.lishangbu.avalon.identity.access.domain.iam

import java.util.UUID

/**
 * 权限聚合标识。
 *
 * @property value 权限主键值。
 */
@JvmInline
value class PermissionId(
    val value: UUID,
)

