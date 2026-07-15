package io.github.lishangbu.scheduler

import tools.jackson.databind.ObjectMapper
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.slf4j.MDC

/**
 * Quartz Job 适配器。
 *
 * JobDetail 中只保存任务 code 和 payload，实际业务逻辑通过 [ScheduledTaskRegistry] 委托给
 * 应用内的 [ScheduledTaskHandler]。
 */
@DisallowConcurrentExecution
class DelegatingScheduledTaskJob(
	private val registry: ScheduledTaskRegistry,
	private val objectMapper: ObjectMapper,
	private val executionRecorder: ScheduledTaskExecutionRecorder = NoopScheduledTaskExecutionRecorder(),
) : Job {
	override fun execute(context: JobExecutionContext) {
		MDC.putCloseable("executionId", context.fireInstanceId).use {
			executeCorrelated(context)
		}
	}

	private fun executeCorrelated(context: JobExecutionContext) {
		val dataMap = context.mergedJobDataMap
		val taskId = dataMap.getString(SCHEDULED_TASK_ID_DATA_KEY)
		val taskCode = dataMap.getString(SCHEDULED_TASK_CODE_DATA_KEY)
		val payload = objectMapper.readPayload(dataMap.getString(SCHEDULED_TASK_PAYLOAD_DATA_KEY))
		val definitionId = dataMap[SCHEDULED_TASK_DEFINITION_ID_DATA_KEY].toLongOrNull()
		val execution = ScheduledTaskExecution(
			taskId = taskId,
			taskCode = taskCode,
			payload = payload,
			scheduledFireTime = context.scheduledFireTime?.toInstant(),
			actualFireTime = context.fireTime.toInstant(),
			refireCount = context.refireCount,
			definitionId = definitionId,
		)

		try {
			val handler = registry.requireHandler(taskCode)
			executionRecorder.record(execution) {
				handler.execute(execution)
			}
		} catch (ex: RuntimeException) {
			throw JobExecutionException(ex, false)
		}
	}

	private fun Any?.toLongOrNull(): Long? =
		when (this) {
			is Number -> toLong()
			is String -> toLongOrNull()
			else -> null
		}
}
