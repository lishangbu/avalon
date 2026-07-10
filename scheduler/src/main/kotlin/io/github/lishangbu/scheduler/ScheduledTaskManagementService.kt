package io.github.lishangbu.scheduler

import tools.jackson.databind.ObjectMapper
import io.github.lishangbu.scheduler.entity.ScheduledTask
import io.github.lishangbu.scheduler.entity.ScheduledTaskExecutionRecord
import io.github.lishangbu.scheduler.entity.actualFireTime
import io.github.lishangbu.scheduler.entity.code
import io.github.lishangbu.scheduler.entity.enabled
import io.github.lishangbu.scheduler.entity.handlerCode
import io.github.lishangbu.scheduler.entity.id
import io.github.lishangbu.scheduler.entity.name
import io.github.lishangbu.scheduler.entity.taskId
import io.github.lishangbu.scheduler.repository.ScheduledTaskExecutionRecordRepository
import io.github.lishangbu.scheduler.repository.ScheduledTaskRepository
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.asc
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.quartz.Scheduler
import org.quartz.TriggerKey
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * 管理端定时任务服务。
 *
 * 服务以 `scheduled_task` 作为业务事实来源，并在任务启用时同步 Quartz Trigger。
 */
open class ScheduledTaskManagementService(
	private val taskRepository: ScheduledTaskRepository,
	private val executionRepository: ScheduledTaskExecutionRecordRepository,
	private val sqlClient: KSqlClient,
	private val operations: ScheduledTaskOperations,
	private val registry: ScheduledTaskRegistry,
	private val scheduler: Scheduler,
	private val objectMapper: ObjectMapper,
) {
	@Transactional(readOnly = true)
	open fun listTasks(page: Int, size: Int, query: String?): Page<ManagedScheduledTaskResponse> {
		requireValidPage(page, size)
		val pattern = query?.trim()?.takeIf { it.isNotBlank() }?.let { "%${it.lowercase()}%" }
		val taskPage = sqlClient.createQuery(ScheduledTask::class) {
			pattern?.let {
				where(
					or(
						table.code ilike it,
						table.name ilike it,
						table.handlerCode ilike it,
					),
				)
			}
			orderBy(table.code)
			select(table)
		}.fetchPage(page, size)
		val lastExecutions = lastExecutionsByTaskId(taskPage.rows.map { it.id })
		return Page(
			taskPage.rows.map { it.toResponse(lastExecutions[it.id]) },
			taskPage.totalRowCount,
			taskPage.totalPageCount,
		)
	}

	@Transactional(readOnly = true)
	open fun getTask(taskId: Long): ManagedScheduledTaskResponse {
		val task = taskById(taskId)
		return task.toResponse(lastExecutionsByTaskId(listOf(task.id))[task.id])
	}

	@Transactional
	open fun createTask(command: SaveManagedScheduledTaskCommand): ManagedScheduledTaskResponse {
		val normalized = command.normalized()
		if (taskExists(normalized.code)) {
			throw ManagedScheduledTaskConflictException("定时任务 code 已存在: ${normalized.code}")
		}
		val task = taskRepository.save(
			ScheduledTask {
				code = normalized.code
				handlerCode = normalized.handlerCode
				name = normalized.name
				description = normalized.description
				groupName = normalized.groupName
				scheduleType = normalized.scheduleType
				cronExpression = normalized.cronExpression
				intervalSeconds = normalized.intervalSeconds
				runAt = normalized.runAt?.toOffsetDateTime()
				timeZone = normalized.timeZone.id
				payloadJson = objectMapper.writePayload(normalized.payload)
				enabled = normalized.enabled
			},
		)
		if (task.enabled) {
			scheduleTask(task)
		}
		return task.toResponse(null)
	}

	@Transactional
	open fun updateTask(taskId: Long, command: SaveManagedScheduledTaskCommand): ManagedScheduledTaskResponse {
		val current = taskById(taskId)
		val normalized = command.normalized()
		if (normalized.code != current.code && taskExists(normalized.code)) {
			throw ManagedScheduledTaskConflictException("定时任务 code 已存在: ${normalized.code}")
		}
		operations.delete(current.reference())
		val updated = taskRepository.save(
			ScheduledTask {
				id = current.id
				code = normalized.code
				handlerCode = normalized.handlerCode
				name = normalized.name
				description = normalized.description
				groupName = normalized.groupName
				scheduleType = normalized.scheduleType
				cronExpression = normalized.cronExpression
				intervalSeconds = normalized.intervalSeconds
				runAt = normalized.runAt?.toOffsetDateTime()
				timeZone = normalized.timeZone.id
				payloadJson = objectMapper.writePayload(normalized.payload)
				enabled = normalized.enabled
			},
		)
		if (updated.enabled) {
			scheduleTask(updated)
		}
		return updated.toResponse(lastExecutionsByTaskId(listOf(updated.id))[updated.id])
	}

	@Transactional
	open fun enableTask(taskId: Long): ManagedScheduledTaskResponse {
		val task = taskById(taskId)
		val enabled = saveEnabled(task, true)
		scheduleTask(enabled)
		return enabled.toResponse(lastExecutionsByTaskId(listOf(enabled.id))[enabled.id])
	}

	@Transactional
	open fun disableTask(taskId: Long): ManagedScheduledTaskResponse {
		val task = taskById(taskId)
		operations.delete(task.reference())
		val disabled = saveEnabled(task, false)
		return disabled.toResponse(lastExecutionsByTaskId(listOf(disabled.id))[disabled.id])
	}

	@Transactional
	open fun deleteTask(taskId: Long): Boolean {
		val task = taskById(taskId)
		operations.delete(task.reference())
		taskRepository.deleteById(task.id)
		return true
	}

	@Transactional
	open fun triggerNow(taskId: Long, payload: Map<String, Any?> = emptyMap()): Boolean {
		val task = taskById(taskId)
		if (!task.enabled) {
			return false
		}
		if (!operations.exists(task.reference())) {
			scheduleTask(task)
		}
		return operations.triggerNow(task.reference(), task.triggerPayload(payload))
	}

	@Transactional(readOnly = true)
	open fun listExecutions(taskId: Long, page: Int, size: Int): Page<ManagedScheduledTaskExecutionResponse> {
		taskById(taskId)
		requireValidPage(page, size)
		return sqlClient.createQuery(ScheduledTaskExecutionRecord::class) {
			where(table.taskId eq taskId)
			orderBy(table.actualFireTime.desc(), table.id.desc())
			select(table)
		}.fetchPage(page, size).let { executionPage ->
			Page(
				executionPage.rows.map { it.toResponse() },
				executionPage.totalRowCount,
				executionPage.totalPageCount,
			)
		}
	}

	@Transactional
	open fun reconcileEnabledTasks() {
		val enabledTasks = sqlClient.executeQuery(ScheduledTask::class) {
			where(table.enabled eq true)
			select(table)
		}
		enabledTasks.forEach(::scheduleTask)
	}

	private fun scheduleTask(task: ScheduledTask) {
		operations.schedule(
			ScheduledTaskRequest(
				taskId = task.code,
				taskCode = task.handlerCode,
				group = task.groupName,
				schedule = task.toSchedule(),
				payload = task.basePayload(),
				description = task.description,
				definitionId = task.id,
			),
		)
	}

	private fun saveEnabled(task: ScheduledTask, enabled: Boolean): ScheduledTask =
		taskRepository.save(
			ScheduledTask {
				id = task.id
				code = task.code
				handlerCode = task.handlerCode
				name = task.name
				description = task.description
				groupName = task.groupName
				scheduleType = task.scheduleType
				cronExpression = task.cronExpression
				intervalSeconds = task.intervalSeconds
				runAt = task.runAt
				timeZone = task.timeZone
				payloadJson = task.payloadJson
				this.enabled = enabled
			},
		)

	private fun SaveManagedScheduledTaskCommand.normalized(): SaveManagedScheduledTaskCommand {
		val normalizedCode = code.requiredSlug("code")
		val normalizedHandlerCode = handlerCode.requiredHandlerCode("handlerCode")
		val normalizedGroup = groupName.requiredSlug("groupName")
		val normalizedName = name.requiredText("name", 120)
		val normalizedDescription = description?.trim()?.takeIf { it.isNotBlank() }?.also {
			if (it.length > 500) {
				throw ManagedScheduledTaskValidationException("description", "description 长度不能超过 500")
			}
		}
		registry.requireHandler(normalizedHandlerCode)
		val normalizedType = scheduleType.trim().uppercase()
		if (normalizedType !in SCHEDULE_TYPES) {
			throw ManagedScheduledTaskValidationException("scheduleType", "scheduleType 不支持: $scheduleType")
		}
		when (normalizedType) {
			"CRON" -> cronExpression.requiredText("cronExpression", 120)
			"FIXED_INTERVAL" -> requireInterval(intervalSeconds)
			"ONCE" -> runAt ?: throw ManagedScheduledTaskValidationException("runAt", "runAt 不能为空")
		}
		return copy(
			code = normalizedCode,
			handlerCode = normalizedHandlerCode,
			name = normalizedName,
			description = normalizedDescription,
			groupName = normalizedGroup,
			scheduleType = normalizedType,
			cronExpression = cronExpression?.trim()?.takeIf { it.isNotBlank() },
			timeZone = timeZone,
		)
	}

	private fun ScheduledTask.toSchedule(): ScheduledTaskSchedule =
		when (scheduleType) {
			"CRON" -> ScheduledTaskSchedule.Cron(
				expression = cronExpression ?: throw ManagedScheduledTaskValidationException("cronExpression", "cronExpression 不能为空"),
				zoneId = ZoneId.of(timeZone),
			)

			"FIXED_INTERVAL" -> ScheduledTaskSchedule.FixedInterval(
				interval = Duration.ofSeconds(intervalSeconds ?: throw ManagedScheduledTaskValidationException("intervalSeconds", "intervalSeconds 不能为空")),
				startAt = Instant.now().plusSeconds(1),
			)

			"ONCE" -> ScheduledTaskSchedule.Once(
				runAt = runAt?.toInstant() ?: throw ManagedScheduledTaskValidationException("runAt", "runAt 不能为空"),
			)

			else -> throw ManagedScheduledTaskValidationException("scheduleType", "scheduleType 不支持: $scheduleType")
		}

	private fun ScheduledTask.toResponse(lastExecution: ScheduledTaskExecutionRecord?): ManagedScheduledTaskResponse {
		val triggerKey = TriggerKey.triggerKey(code, groupName)
		val trigger = scheduler.getTrigger(triggerKey)
		val triggerState = trigger?.let { scheduler.getTriggerState(triggerKey).name }
		return ManagedScheduledTaskResponse {
			id = this@toResponse.id
			code = this@toResponse.code
			handlerCode = this@toResponse.handlerCode
			name = this@toResponse.name
			description = this@toResponse.description
			groupName = this@toResponse.groupName
			scheduleType = this@toResponse.scheduleType
			cronExpression = this@toResponse.cronExpression
			intervalSeconds = this@toResponse.intervalSeconds
			runAt = this@toResponse.runAt?.toInstant()
			timeZone = this@toResponse.timeZone
			payload = this@toResponse.basePayload()
			enabled = this@toResponse.enabled
			nextFireTime = trigger?.nextFireTime?.toInstant()
			this.triggerState = triggerState
			lastExecutionStatus = lastExecution?.status
			lastExecutionAt = lastExecution?.actualFireTime?.toInstant()
		}
	}

	private fun ScheduledTaskExecutionRecord.toResponse(): ManagedScheduledTaskExecutionResponse =
		ManagedScheduledTaskExecutionResponse {
			id = this@toResponse.id
			taskId = this@toResponse.taskId
			taskCode = this@toResponse.taskCode
			handlerCode = this@toResponse.handlerCode
			scheduledFireTime = this@toResponse.scheduledFireTime?.toInstant()
			actualFireTime = this@toResponse.actualFireTime.toInstant()
			finishedAt = this@toResponse.finishedAt?.toInstant()
			status = this@toResponse.status
			durationMs = this@toResponse.durationMs
			refireCount = this@toResponse.refireCount
			payloadSnapshot = objectMapper.readPayload(this@toResponse.payloadSnapshotJson)
			errorMessage = this@toResponse.errorMessage
		}

	private fun ScheduledTask.basePayload(): Map<String, Any?> =
		objectMapper.readPayload(payloadJson)

	private fun ScheduledTask.triggerPayload(payload: Map<String, Any?>): Map<String, Any?> =
		basePayload() + payload

	private fun ScheduledTask.reference(): ScheduledTaskReference =
		ScheduledTaskReference(taskId = code, group = groupName)

	private fun taskById(taskId: Long): ScheduledTask =
		taskRepository.findNullable(taskId) ?: throw ManagedScheduledTaskNotFoundException(taskId)

	private fun taskExists(code: String): Boolean =
		sqlClient.createQuery(ScheduledTask::class) {
			where(table.code eq code)
			select(table.id)
		}.exists()

	private fun lastExecutionsByTaskId(taskIds: List<Long>): Map<Long, ScheduledTaskExecutionRecord> {
		if (taskIds.isEmpty()) {
			return emptyMap()
		}
		return sqlClient.executeQuery(ScheduledTaskExecutionRecord::class) {
			where(table.taskId valueIn taskIds)
			orderBy(table.taskId.asc(), table.actualFireTime.desc(), table.id.desc())
			select(table)
		}.distinctBy { it.taskId }
			.associateBy { it.taskId }
	}

	private fun requireValidPage(page: Int, size: Int) {
		if (page < 0) {
			throw ManagedScheduledTaskValidationException("page", "page 不能小于 0")
		}
		if (size !in 1..100) {
			throw ManagedScheduledTaskValidationException("size", "size 必须在 1 到 100 之间")
		}
	}

	private fun requireInterval(intervalSeconds: Long?) {
		if (intervalSeconds == null || intervalSeconds <= 0) {
			throw ManagedScheduledTaskValidationException("intervalSeconds", "intervalSeconds 必须大于 0")
		}
	}

	private fun String?.requiredText(field: String, maxLength: Int): String {
		val value = orEmpty().trim()
		if (value.isBlank()) {
			throw ManagedScheduledTaskValidationException(field, "$field 不能为空")
		}
		if (value.length > maxLength) {
			throw ManagedScheduledTaskValidationException(field, "$field 长度不能超过 $maxLength")
		}
		return value
	}

	private fun String.requiredSlug(field: String): String {
		val value = requiredText(field, 100)
		if (!SLUG_PATTERN.matches(value)) {
			throw ManagedScheduledTaskValidationException(field, "$field 必须是小写 slug")
		}
		return value
	}

	private fun String.requiredHandlerCode(field: String): String {
		val value = requiredText(field, 100)
		if (!HANDLER_CODE_PATTERN.matches(value)) {
			throw ManagedScheduledTaskValidationException(field, "$field 必须是稳定处理器 code")
		}
		return value
	}

	private fun Instant.toOffsetDateTime(): OffsetDateTime =
		OffsetDateTime.ofInstant(this, ZoneOffset.UTC)

	private companion object {
		private val SLUG_PATTERN = Regex("^[a-z][a-z0-9-]{2,99}$")
		private val HANDLER_CODE_PATTERN = Regex("^[a-z][a-z0-9-]*(\\.[a-z][a-z0-9-]*)+$")
		private val SCHEDULE_TYPES = setOf("ONCE", "FIXED_INTERVAL", "CRON")
	}
}
