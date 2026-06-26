package io.github.lishangbu.security

import io.github.lishangbu.scheduler.ScheduledTaskExecution
import io.github.lishangbu.scheduler.ScheduledTaskHandler
import org.springframework.stereotype.Component
import java.util.concurrent.CountDownLatch

@Component
class ManagementApiScheduledTaskHandler : ScheduledTaskHandler {
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
