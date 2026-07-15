package io.github.lishangbu.system.audit

data class AdminAuditCommand(
	val actorAccountId: Long,
	val httpMethod: String,
	val requestPath: String,
	val outcome: String,
	val responseStatus: Int,
	val requestId: String,
	val remoteAddress: String,
)
