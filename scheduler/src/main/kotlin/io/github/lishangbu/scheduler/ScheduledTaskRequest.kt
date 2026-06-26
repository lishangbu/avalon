package io.github.lishangbu.scheduler

/**
 * 创建或更新定时任务的请求。
 *
 * [taskId] 在同一 [group] 内唯一；重复调度同一任务会替换已有 Quartz Job 和 Trigger。
 */
data class ScheduledTaskRequest(
	val taskId: String,
	val taskCode: String,
	val schedule: ScheduledTaskSchedule,
	val group: String = DEFAULT_SCHEDULED_TASK_GROUP,
	val payload: Map<String, Any?> = emptyMap(),
	val description: String? = null,
	val definitionId: Long? = null,
)
