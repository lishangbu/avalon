package io.github.lishangbu.scheduler

/**
 * 已注册定时任务的稳定引用。
 */
data class ScheduledTaskReference(
	val taskId: String,
	val group: String = DEFAULT_SCHEDULED_TASK_GROUP,
)
