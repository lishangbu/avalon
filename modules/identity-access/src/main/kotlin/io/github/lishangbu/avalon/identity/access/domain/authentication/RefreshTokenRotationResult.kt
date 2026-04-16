package io.github.lishangbu.avalon.identity.access.domain.authentication

/**
 * refresh token rotation 成功后的装配结果。
 *
 * @property user 当前会话所属用户。
 * @property session 已更新后的会话视图。
 */
data class RefreshTokenRotationResult(
    val user: AuthenticationUser,
    val session: UserSession,
)