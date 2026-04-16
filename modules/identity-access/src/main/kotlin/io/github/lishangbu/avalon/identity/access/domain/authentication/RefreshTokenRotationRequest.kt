package io.github.lishangbu.avalon.identity.access.domain.authentication

import java.time.Instant

/**
 * 执行 refresh token rotation 时使用的事务输入。
 *
 * @property currentTokenHash 当前 refresh token 的散列值。
 * @property newTokenHash 新 refresh token 的散列值。
 * @property rotatedAt 轮换发生时间。
 * @property newRefreshTokenExpiresAt 新 refresh token 的过期时间。
 */
data class RefreshTokenRotationRequest(
    val currentTokenHash: String,
    val newTokenHash: String,
    val rotatedAt: Instant,
    val newRefreshTokenExpiresAt: Instant,
)