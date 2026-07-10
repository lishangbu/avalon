package io.github.lishangbu.scheduler

/**
 * 管理端定时任务定义不符合调度规则。
 */
class ManagedScheduledTaskValidationException(
	val field: String,
	message: String,
) : RuntimeException(message)
