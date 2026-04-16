package io.github.lishangbu.avalon.identity.access.domain.iam

import java.util.UUID

/**
 * 用户聚合标识。
 *
 * @property value 用户主键值。
 */
@JvmInline
value class UserId(
    val value: UUID,
)

