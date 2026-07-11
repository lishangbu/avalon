package io.github.lishangbu.battlesession

import java.time.Instant

data class SessionTermination(
	val commandId: String,
	val reason: String,
	val revisionBefore: Long,
	val revisionAfter: Long,
	val terminatedAt: Instant,
)
