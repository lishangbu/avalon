package io.github.lishangbu.scheduler

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import java.util.concurrent.CountDownLatch

@SpringBootConfiguration
@EnableAutoConfiguration
class SchedulerManagementTestApplication {
	@Bean
	fun schedulerManagementTestHandler(): ScheduledTaskHandler =
		SchedulerManagementTestHandler()
}

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
