package io.github.lishangbu.scheduler

import java.time.Instant
import java.time.ZoneId

/**
 * 创建或更新管理端定时任务定义的命令。
 */
data class SaveManagedScheduledTaskCommand(
	val code: String,
	val handlerCode: String,
	val name: String,
	val description: String?,
	val groupName: String = DEFAULT_SCHEDULED_TASK_GROUP,
	val scheduleType: String,
	val cronExpression: String?,
	val intervalSeconds: Long?,
	val runAt: Instant?,
	val timeZone: ZoneId = ZoneId.of("UTC"),
	val payload: Map<String, Any?>,
	val enabled: Boolean,
)
