package io.github.lishangbu.scheduler

import java.time.Instant

/**
 * 定时任务处理器收到的执行上下文。
 *
 * 该上下文只暴露业务任务标识、处理器 code、JSON payload 和 Quartz 火发时间，
 * 避免业务处理器直接依赖 Quartz 的运行时对象。
 */
data class ScheduledTaskExecution(
	val taskId: String,
	val taskCode: String,
	val payload: Map<String, Any?>,
	val scheduledFireTime: Instant?,
	val actualFireTime: Instant,
	val refireCount: Int,
	val definitionId: Long? = null,
)
