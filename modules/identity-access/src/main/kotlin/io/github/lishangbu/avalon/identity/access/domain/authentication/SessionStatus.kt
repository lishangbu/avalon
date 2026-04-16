package io.github.lishangbu.avalon.identity.access.domain.authentication

/**
 * 会话生命周期状态。
 */
enum class SessionStatus {
    ACTIVE,
    REVOKED,
    EXPIRED,
}