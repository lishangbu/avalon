package io.github.lishangbu.avalon.oauth2.common.log

import java.time.Instant

@JvmRecord
data class AuthenticationLogRecord(
    /** 用户名 */
    val username: String?,
    /** 客户端 ID */
    val clientId: String?,
    /** 授权属性 */
    val grantType: String?,
    /** IP */
    val ip: String?,
    /** 用户代理 */
    val userAgent: String?,
    /** 成功 */
    val success: Boolean,
    /** 错误信息 */
    val errorMessage: String?,
    /** 时间戳 */
    val timestamp: Instant?,
)
