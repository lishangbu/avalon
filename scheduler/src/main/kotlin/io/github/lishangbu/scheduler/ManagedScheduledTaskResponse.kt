package io.github.lishangbu.scheduler

import java.time.Instant

/**
 * 管理端定时任务定义响应。
 */
data class ManagedScheduledTaskResponse(
	val id: Long,
	val code: String,
	val handlerCode: String,
	val name: String,
	val description: String?,
	val groupName: String,
	val scheduleType: String,
	val cronExpression: String?,
	val intervalSeconds: Long?,
	val runAt: Instant?,
	val timeZone: String,
	val payload: Map<String, Any?>,
	val enabled: Boolean,
	val nextFireTime: Instant?,
	val triggerState: String?,
	val lastExecutionStatus: String?,
	val lastExecutionAt: Instant?,
	val createdAt: Instant,
	val updatedAt: Instant,
)
