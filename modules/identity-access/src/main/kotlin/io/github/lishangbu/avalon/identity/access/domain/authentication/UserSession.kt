package io.github.lishangbu.avalon.identity.access.domain.authentication

import io.github.lishangbu.avalon.identity.access.domain.iam.UserId
import java.time.Instant
import java.util.UUID

/**
 * 持久化后的用户会话视图。
 *
 * @property id 会话主键。
 * @property sessionKey 会话稳定键。
 * @property userId 所属用户标识。
 * @property clientType 客户端类型。
 * @property deviceName 设备名称，可为空。
 * @property deviceFingerprint 设备指纹，可为空。
 * @property userAgent 原始 User-Agent，可为空。
 * @property ip 客户端 IP，可为空。
 * @property status 当前会话状态。
 * @property revokedReason 会话撤销原因，可为空。
 * @property lastAuthenticatedAt 最近一次成功认证时间。
 * @property lastRefreshedAt 最近一次 refresh token 轮换时间。
 * @property expiresAt 会话整体过期时间。
 * @property version 乐观锁版本号。
 */
data class UserSession(
    val id: UUID,
    val sessionKey: String,
    val userId: UserId,
    val clientType: ClientType,
    val deviceName: String?,
    val deviceFingerprint: String?,
    val userAgent: String?,
    val ip: String?,
    val status: SessionStatus,
    val revokedReason: SessionRevokedReason?,
    val lastAuthenticatedAt: Instant,
    val lastRefreshedAt: Instant,
    val expiresAt: Instant,
    val version: Long,
)

