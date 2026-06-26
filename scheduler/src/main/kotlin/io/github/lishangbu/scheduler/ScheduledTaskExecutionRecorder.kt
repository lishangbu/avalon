package io.github.lishangbu.scheduler

/**
 * 记录调度任务执行状态的扩展点。
 */
interface ScheduledTaskExecutionRecorder {
	fun record(execution: ScheduledTaskExecution, executeHandler: () -> Unit)
}
