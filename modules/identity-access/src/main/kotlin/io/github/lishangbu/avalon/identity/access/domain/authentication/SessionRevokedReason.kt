package io.github.lishangbu.avalon.identity.access.domain.authentication

/**
 * 会话被撤销时的原因分类。
 */
enum class SessionRevokedReason {
    LOGOUT,
    LOGOUT_ALL,
    SESSION_LIMIT_EVICTED,
    TOKEN_REUSE_DETECTED,
    USER_DISABLED,
}