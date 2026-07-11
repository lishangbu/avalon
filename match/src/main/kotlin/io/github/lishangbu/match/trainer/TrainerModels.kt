package io.github.lishangbu.match.trainer

import java.time.Instant
import java.util.UUID

data class TrainerRecord(
	val id: Long,
	val accountId: Long,
	val displayName: String,
	val displayNameKey: String,
	val commandId: String,
	val revision: Long,
	val archivedAt: Instant?,
)

data class CreateTrainerCommand(val commandId: UUID, val displayName: String)

enum class SensitiveNameMatchType { EXACT, CONTAINS }

data class SensitiveNameRule(val normalizedTerm: String, val matchType: SensitiveNameMatchType)
