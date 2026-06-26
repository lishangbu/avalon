package io.github.lishangbu.scheduler

import java.time.Instant

/**
 * 管理端定时任务执行记录响应。
 */
data class ManagedScheduledTaskExecutionResponse(
	val id: Long,
	val taskId: Long,
	val taskCode: String,
	val handlerCode: String,
	val scheduledFireTime: Instant?,
	val actualFireTime: Instant,
	val finishedAt: Instant?,
	val status: String,
	val durationMs: Long?,
	val refireCount: Int,
	val payloadSnapshot: Map<String, Any?>,
	val errorMessage: String?,
)
