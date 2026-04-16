package io.github.lishangbu.avalon.identity.access.domain.authentication

/**
 * refresh token 生命周期状态。
 */
enum class RefreshTokenStatus {
    ACTIVE,
    ROTATED,
    REVOKED,
    EXPIRED,
}