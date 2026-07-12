package io.github.lishangbu.match.trainer

import java.time.Instant

interface TrainerStore {
	fun lockAccount(accountId: Long)
	fun findByCommand(accountId: Long, commandId: String): TrainerRecord?
	fun findById(accountId: Long, trainerId: Long): TrainerRecord?
	fun list(accountId: Long): List<TrainerRecord>
	fun countActive(accountId: Long): Int
	fun hasBlockingActivity(accountId: Long, trainerId: Long): Boolean
	fun findActiveMatchTrainerId(accountId: Long): Long?
	fun enabledSensitiveNameRules(): List<SensitiveNameRule>
	fun insert(record: TrainerRecord): TrainerRecord
	fun archive(accountId: Long, trainerId: Long, expectedRevision: Long, archivedAt: Instant): TrainerRecord?
	fun restore(accountId: Long, trainerId: Long, expectedRevision: Long): TrainerRecord?
}
