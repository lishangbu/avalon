package io.github.lishangbu.scheduler

/**
 * 管理端定时任务定义不存在。
 */
class ManagedScheduledTaskNotFoundException(
	taskId: Long,
) : RuntimeException("定时任务不存在: $taskId")

/**
 * 管理端定时任务定义发生唯一键冲突。
 */
class ManagedScheduledTaskConflictException(
	message: String,
) : RuntimeException(message)

/**
 * 管理端定时任务定义不符合调度规则。
 */
class ManagedScheduledTaskValidationException(
	val field: String,
	message: String,
) : RuntimeException(message)
