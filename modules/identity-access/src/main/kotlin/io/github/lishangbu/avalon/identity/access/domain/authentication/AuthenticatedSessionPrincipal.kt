package io.github.lishangbu.avalon.identity.access.domain.authentication

import io.github.lishangbu.avalon.identity.access.domain.iam.UserId

/**
 * 当前已认证会话在请求链路中的最小主体。
 *
 * @property userId 当前用户标识。
 * @property sessionId 当前会话键。
 */
data class AuthenticatedSessionPrincipal(
    val userId: UserId,
    val sessionId: String,
)