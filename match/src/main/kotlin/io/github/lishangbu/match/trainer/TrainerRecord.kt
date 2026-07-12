package io.github.lishangbu.match.trainer

import java.time.Instant

/** Trainer 持久化实体在领域服务和 API 层使用的只读快照。 */
data class TrainerRecord(
	val id: Long,
	val accountId: Long,
	val displayName: String,
	val displayNameKey: String,
	val commandId: String,
	val revision: Long,
	val archivedAt: Instant?,
)
