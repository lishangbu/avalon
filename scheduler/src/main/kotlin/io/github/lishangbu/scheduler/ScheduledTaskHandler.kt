package io.github.lishangbu.scheduler

/**
 * 可被 Quartz 调度的业务任务处理器。
 *
 * 每个处理器必须提供稳定唯一的 [code]，调度请求通过该 code 查找真正的业务执行逻辑。
 */
interface ScheduledTaskHandler {
	val code: String

	fun execute(execution: ScheduledTaskExecution)
}
