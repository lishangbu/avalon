package io.github.lishangbu.avalon.oauth2.common.log

import java.time.Instant

@JvmRecord
data class AuthenticationLogRecord(
    val username: String?,
    val clientId: String?,
    val grantType: String?,
    val ip: String?,
    val userAgent: String?,
    val success: Boolean,
    val errorMessage: String?,
    val timestamp: Instant?,
)
