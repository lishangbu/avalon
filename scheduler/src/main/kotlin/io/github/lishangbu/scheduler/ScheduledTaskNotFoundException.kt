package io.github.lishangbu.scheduler

/**
 * 调度请求引用了未注册的任务处理器 code。
 */
class ScheduledTaskNotFoundException(
	taskCode: String,
) : RuntimeException("未找到定时任务处理器: $taskCode")
