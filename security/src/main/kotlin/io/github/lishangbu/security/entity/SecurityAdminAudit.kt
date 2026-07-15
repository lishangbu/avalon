package io.github.lishangbu.security.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.Instant

/** Append-only record of an administrative mutation at the HTTP boundary. */
@Entity
@Table(name = "security_admin_audit")
interface SecurityAdminAudit {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long
	val actorAccountId: Long
	val httpMethod: String
	val requestPath: String
	val outcome: String
	val responseStatus: Int
	val requestId: String
	val remoteAddress: String
	val occurredAt: Instant
}
