package io.github.lishangbu.scheduler

/**
 * 管理端定时任务定义发生唯一键冲突。
 */
class ManagedScheduledTaskConflictException(
	message: String,
) : RuntimeException(message)
