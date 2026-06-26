package io.github.lishangbu.scheduler

import com.fasterxml.jackson.databind.ObjectMapper
import org.quartz.CronScheduleBuilder
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.SimpleScheduleBuilder
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.quartz.TriggerKey
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.TimeZone

/**
 * 基于 Quartz Scheduler 的定时任务操作实现。
 */
class QuartzScheduledTaskOperations(
	private val scheduler: Scheduler,
	private val registry: ScheduledTaskRegistry,
	private val objectMapper: ObjectMapper,
) : ScheduledTaskOperations {
	override fun schedule(request: ScheduledTaskRequest): ScheduledTaskReference {
		val taskId = normalizeName(request.taskId, "taskId")
		val group = normalizeName(request.group, "group")
		val taskCode = normalizeName(request.taskCode, "taskCode")
		registry.requireHandler(taskCode)

		val reference = ScheduledTaskReference(taskId, group)
		val jobDetail = JobBuilder.newJob(DelegatingScheduledTaskJob::class.java)
			.withIdentity(reference.jobKey())
			.usingJobData(SCHEDULED_TASK_ID_DATA_KEY, taskId)
			.usingJobData(SCHEDULED_TASK_CODE_DATA_KEY, taskCode)
			.usingJobData(SCHEDULED_TASK_PAYLOAD_DATA_KEY, objectMapper.writeValueAsString(request.payload))
			.storeDurably(true)
			.requestRecovery(true)
			.also { builder ->
				request.definitionId?.let { builder.usingJobData(SCHEDULED_TASK_DEFINITION_ID_DATA_KEY, it) }
			}
			.also { builder ->
				request.description?.trim()?.takeIf { it.isNotBlank() }?.let(builder::withDescription)
			}
			.build()
		val trigger = request.schedule.toTrigger(reference, request.payload)

		if (scheduler.checkExists(reference.jobKey())) {
			scheduler.addJob(jobDetail, true, true)
			if (scheduler.checkExists(reference.triggerKey())) {
				scheduler.rescheduleJob(reference.triggerKey(), trigger)
			} else {
				scheduler.scheduleJob(trigger)
			}
		} else {
			scheduler.scheduleJob(jobDetail, trigger)
		}
		return reference
	}

	override fun triggerNow(
		reference: ScheduledTaskReference,
		payload: Map<String, Any?>,
	): Boolean {
		val normalizedReference = reference.normalized()
		if (!scheduler.checkExists(normalizedReference.jobKey())) {
			return false
		}
		scheduler.triggerJob(
			normalizedReference.jobKey(),
			JobDataMap(
				mapOf(
					SCHEDULED_TASK_PAYLOAD_DATA_KEY to objectMapper.writeValueAsString(payload),
				),
			),
		)
		return true
	}

	override fun pause(reference: ScheduledTaskReference): Boolean {
		val normalizedReference = reference.normalized()
		if (!scheduler.checkExists(normalizedReference.triggerKey())) {
			return false
		}
		scheduler.pauseTrigger(normalizedReference.triggerKey())
		return true
	}

	override fun resume(reference: ScheduledTaskReference): Boolean {
		val normalizedReference = reference.normalized()
		if (!scheduler.checkExists(normalizedReference.triggerKey())) {
			return false
		}
		scheduler.resumeTrigger(normalizedReference.triggerKey())
		return true
	}

	override fun delete(reference: ScheduledTaskReference): Boolean {
		val normalizedReference = reference.normalized()
		return scheduler.deleteJob(normalizedReference.jobKey())
	}

	override fun exists(reference: ScheduledTaskReference): Boolean =
		scheduler.checkExists(reference.normalized().jobKey())

	private fun ScheduledTaskSchedule.toTrigger(
		reference: ScheduledTaskReference,
		payload: Map<String, Any?>,
	): Trigger {
		val builder = TriggerBuilder.newTrigger()
			.withIdentity(reference.triggerKey())
			.forJob(reference.jobKey())
			.usingJobData(SCHEDULED_TASK_PAYLOAD_DATA_KEY, objectMapper.writeValueAsString(payload))

		return when (this) {
			is ScheduledTaskSchedule.Once -> builder
				.startAt(Date.from(runAt))
				.withSchedule(
					SimpleScheduleBuilder.simpleSchedule()
						.withRepeatCount(0)
						.withMisfireHandlingInstructionFireNow(),
				)
				.build()

			is ScheduledTaskSchedule.FixedInterval -> builder
				.startAt(Date.from(startAt ?: Instant.now()))
				.withSchedule(
					SimpleScheduleBuilder.simpleSchedule()
						.withIntervalInMilliseconds(interval.toPositiveMilliseconds())
						.repeatForever()
						.withMisfireHandlingInstructionNextWithRemainingCount(),
				)
				.build()

			is ScheduledTaskSchedule.Cron -> builder
				.startAt(Date.from(startAt ?: Instant.now()))
				.withSchedule(
					CronScheduleBuilder.cronSchedule(normalizeName(expression, "cron expression"))
						.inTimeZone(TimeZone.getTimeZone(zoneId))
						.withMisfireHandlingInstructionDoNothing(),
				)
				.build()
		}
	}

	private fun ScheduledTaskReference.normalized(): ScheduledTaskReference =
		ScheduledTaskReference(
			taskId = normalizeName(taskId, "taskId"),
			group = normalizeName(group, "group"),
		)

	private fun ScheduledTaskReference.jobKey(): JobKey =
		JobKey.jobKey(taskId, group)

	private fun ScheduledTaskReference.triggerKey(): TriggerKey =
		TriggerKey.triggerKey(taskId, group)

	private fun Duration.toPositiveMilliseconds(): Long {
		val milliseconds = toMillis()
		require(milliseconds > 0) { "固定间隔任务的 interval 必须至少为 1 毫秒" }
		return milliseconds
	}

	private fun normalizeName(
		value: String,
		fieldName: String,
	): String =
		value.trim().also {
			require(it.isNotBlank()) { "$fieldName 不能为空" }
		}
}
