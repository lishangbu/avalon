package io.github.lishangbu.avalon.identity.access.domain.authentication

import java.time.Instant

/**
 * 创建会话与首个 refresh token 时使用的事务输入。
 *
 * @property sessionKey 会话稳定键。
 * @property clientType 客户端类型。
 * @property deviceName 设备名称，可为空。
 * @property deviceFingerprint 设备指纹，可为空。
 * @property userAgent 原始 User-Agent，可为空。
 * @property ip 客户端 IP，可为空。
 * @property issuedAt 本次登录成功时间。
 * @property refreshTokenHash 首枚 refresh token 的散列值。
 * @property refreshTokenExpiresAt 首枚 refresh token 的过期时间。
 * @property maxActiveSessions 允许同时保留的最大活跃会话数。
 */
data class SessionCreationRequest(
    val sessionKey: String,
    val clientType: ClientType,
    val deviceName: String?,
    val deviceFingerprint: String?,
    val userAgent: String?,
    val ip: String?,
    val issuedAt: Instant,
    val refreshTokenHash: String,
    val refreshTokenExpiresAt: Instant,
    val maxActiveSessions: Int,
)