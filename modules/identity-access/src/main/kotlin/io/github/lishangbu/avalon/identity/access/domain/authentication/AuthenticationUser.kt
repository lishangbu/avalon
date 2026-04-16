package io.github.lishangbu.avalon.identity.access.domain.authentication

import io.github.lishangbu.avalon.identity.access.domain.iam.UserId

/**
 * 用于认证流程的最小用户快照。
 *
 * @property id 用户标识。
 * @property username 用户名。
 * @property enabled 用户是否启用。
 * @property passwordHash 已保存的密码散列，可为空。
 */
data class AuthenticationUser(
    val id: UserId,
    val username: String,
    val enabled: Boolean,
    val passwordHash: String?,
)