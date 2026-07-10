package io.github.lishangbu.scheduler

import tools.jackson.databind.ObjectMapper
import io.github.lishangbu.scheduler.entity.ScheduledTaskExecutionRecord
import io.github.lishangbu.scheduler.repository.ScheduledTaskExecutionRecordRepository
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * 将托管定时任务的执行过程写入管理端执行日志。
 */
open class JimmerScheduledTaskExecutionRecorder(
	private val repository: ScheduledTaskExecutionRecordRepository,
	private val objectMapper: ObjectMapper,
) : ScheduledTaskExecutionRecorder {
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	override fun record(execution: ScheduledTaskExecution, executeHandler: () -> Unit) {
		val taskId = execution.definitionId
		if (taskId == null) {
			executeHandler()
			return
		}

		val start = OffsetDateTime.now(ZoneOffset.UTC)
		val running = repository.save(
			ScheduledTaskExecutionRecord {
				this.taskId = taskId
				taskCode = execution.taskId
				handlerCode = execution.taskCode
				scheduledFireTime = execution.scheduledFireTime?.toOffsetDateTime()
				actualFireTime = execution.actualFireTime.toOffsetDateTime()
				finishedAt = null
				status = "RUNNING"
				durationMs = null
				refireCount = execution.refireCount
				payloadSnapshotJson = objectMapper.writePayload(execution.payload)
				errorMessage = null
			},
			SaveMode.INSERT_ONLY,
		)

		try {
			executeHandler()
			finish(running, "SUCCESS", start, null)
		} catch (ex: RuntimeException) {
			finish(running, "FAILED", start, ex.message?.take(ERROR_MESSAGE_MAX_LENGTH))
			throw ex
		}
	}

	private fun finish(
		record: ScheduledTaskExecutionRecord,
		status: String,
		start: OffsetDateTime,
		errorMessage: String?,
	) {
		val finishedAt = OffsetDateTime.now(ZoneOffset.UTC)
		repository.save(
			ScheduledTaskExecutionRecord {
				id = record.id
				taskId = record.taskId
				taskCode = record.taskCode
				handlerCode = record.handlerCode
				scheduledFireTime = record.scheduledFireTime
				actualFireTime = record.actualFireTime
				this.finishedAt = finishedAt
				this.status = status
				durationMs = Duration.between(start, finishedAt).toMillis().coerceAtLeast(0)
				refireCount = record.refireCount
				payloadSnapshotJson = record.payloadSnapshotJson
				this.errorMessage = errorMessage
			},
		)
	}

	private fun java.time.Instant.toOffsetDateTime(): OffsetDateTime =
		OffsetDateTime.ofInstant(this, ZoneOffset.UTC)

	private companion object {
		private const val ERROR_MESSAGE_MAX_LENGTH = 1000
	}
}
