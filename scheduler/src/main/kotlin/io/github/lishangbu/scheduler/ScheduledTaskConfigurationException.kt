package io.github.lishangbu.scheduler

/**
 * 定时任务模块启动或注册阶段发现不可恢复的配置错误。
 */
class ScheduledTaskConfigurationException(
	message: String,
) : RuntimeException(message)
