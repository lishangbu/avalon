package io.github.lishangbu.scheduler

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * 封装对外可用的调度表达方式。
 *
 * Quartz 的具体 Trigger 类型保留在模块内部，调用方只需要选择一次性、固定间隔或 Cron 调度。
 */
sealed interface ScheduledTaskSchedule {
	data class Once(
		val runAt: Instant,
	) : ScheduledTaskSchedule

	data class FixedInterval(
		val interval: Duration,
		val startAt: Instant? = null,
	) : ScheduledTaskSchedule

	data class Cron(
		val expression: String,
		val zoneId: ZoneId = ZoneId.systemDefault(),
		val startAt: Instant? = null,
	) : ScheduledTaskSchedule
}
