package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.AuthenticationLog
import io.github.lishangbu.avalon.authorization.repository.AuthenticationLogRepository
import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecord
import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecorder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class DefaultAuthenticationLogRecorder(
    /** 认证日志仓储 */
    private val authenticationLogRepository: AuthenticationLogRepository,
) : AuthenticationLogRecorder {
    /** 记录信息 */
    override fun record(record: AuthenticationLogRecord) {
        val log =
            AuthenticationLog {
                username = record.username
                clientId = record.clientId
                grantType = record.grantType
                ip = record.ip
                userAgent = record.userAgent
                success = record.success
                errorMessage = record.errorMessage
                occurredAt = record.timestamp ?: Instant.now()
            }
        authenticationLogRepository.save(log)
    }
}
