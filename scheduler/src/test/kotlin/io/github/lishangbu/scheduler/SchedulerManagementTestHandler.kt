package io.github.lishangbu.scheduler

import java.util.concurrent.CountDownLatch

/**
 * 记录调度执行结果的测试处理器。
 *
 * `latch` 用于让集成测试等待异步 Quartz 调用完成，`executions` 保存本次测试实际收到的执行上下文。
 */
class SchedulerManagementTestHandler : ScheduledTaskHandler {
	override val code: String = "test.echo"

	override fun execute(execution: ScheduledTaskExecution) {
		executions += execution
		latch.countDown()
	}

	companion object {
		lateinit var latch: CountDownLatch
		val executions: MutableList<ScheduledTaskExecution> = mutableListOf()
	}
}
