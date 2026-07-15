package io.github.lishangbu.system.audit

import io.github.lishangbu.security.entity.SecurityAdminAudit
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class AdminAuditService(
	private val sqlClient: KSqlClient,
	private val clock: Clock,
) {
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	fun append(command: AdminAuditCommand) {
		sqlClient.save(
			SecurityAdminAudit {
				actorAccountId = command.actorAccountId
				httpMethod = command.httpMethod
				requestPath = command.requestPath
				outcome = command.outcome
				responseStatus = command.responseStatus
				requestId = command.requestId
				remoteAddress = command.remoteAddress
				occurredAt = Instant.now(clock)
			},
		) { setMode(SaveMode.INSERT_ONLY) }
	}
}
