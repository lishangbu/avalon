package io.github.lishangbu.scheduler

/**
 * 管理端定时任务定义不存在。
 */
class ManagedScheduledTaskNotFoundException(
	taskId: Long,
) : RuntimeException("定时任务不存在: $taskId")
