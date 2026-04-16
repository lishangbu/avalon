package io.github.lishangbu.avalon.identity.access.domain.authentication

import io.github.lishangbu.avalon.identity.access.domain.iam.UserId
import java.time.Instant

/**
 * 认证审计日志条目。
 *
 * @property clientId OAuth 风格的客户端标识，可为空。
 * @property errorMessage 失败原因摘要，可为空。
 * @property grantType 认证授权类型，可为空。
 * @property identityType 本次使用的登录标识类型，可为空。
 * @property ip 客户端 IP，可为空。
 * @property occurredAt 日志发生时间。
 * @property principal 原始 principal，可为空。
 * @property resultCode 稳定结果码。
 * @property sessionId 会话键，可为空。
 * @property success 本次认证是否成功。
 * @property userAgent 原始 User-Agent，可为空。
 * @property userId 命中的用户标识，可为空。
 * @property username 命中的用户名，可为空。
 */
data class AuthenticationLogEntry(
    val clientId: String?,
    val errorMessage: String?,
    val grantType: String?,
    val identityType: IdentityType?,
    val ip: String?,
    val occurredAt: Instant,
    val principal: String?,
    val resultCode: String,
    val sessionId: String?,
    val success: Boolean,
    val userAgent: String?,
    val userId: UserId?,
    val username: String?,
)