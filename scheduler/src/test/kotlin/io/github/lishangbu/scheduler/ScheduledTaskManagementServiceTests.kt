package io.github.lishangbu.scheduler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest(
	classes = [SchedulerManagementTestApplication::class],
	properties = [
		"spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml",
		"spring.quartz.job-store-type=memory",
		"spring.quartz.auto-startup=true",
		"jimmer.language=kotlin",
		"jimmer.dialect=org.babyfish.jimmer.sql.dialect.PostgresDialect",
		"cosid.machine.enabled=true",
		"cosid.machine.distributor.manual.machine-id=1",
		"cosid.snowflake.enabled=true",
		"cosid.snowflake.zone-id=UTC",
	],
)
@ContextConfiguration(initializers = [SchedulerManagementPostgresTestContainer::class])
class ScheduledTaskManagementServiceTests(
	@Autowired private val service: ScheduledTaskManagementService,
) {
	@BeforeEach
	fun resetHandler() {
		SchedulerManagementTestHandler.latch = CountDownLatch(1)
		SchedulerManagementTestHandler.executions.clear()
	}

	@Test
	fun `create trigger and list execution records for managed task`() {
		val task = service.createTask(
			SaveManagedScheduledTaskCommand(
				code = "cleanup-expired-token",
				handlerCode = "test.echo",
				name = "Cleanup Expired Token",
				description = "Remove expired authorization state",
				groupName = "system",
				scheduleType = "CRON",
				cronExpression = "* * * * * ?",
				intervalSeconds = null,
				runAt = null,
				timeZone = ZoneId.of("UTC"),
				payload = mapOf("scope" to "expired-token"),
				enabled = true,
			),
		)
		val scheduledExecution = awaitSuccessfulExecution(task.id, "expired-token")

		val triggered = service.triggerNow(task.id, mapOf("scope" to "manual"))

		assertThat(triggered).isTrue()
		val manualExecution = awaitSuccessfulExecution(task.id, "manual")
		service.disableTask(task.id)
		val executions = service.listExecutions(task.id, 0, 10).rows
		assertThat(scheduledExecution.taskCode).isEqualTo("cleanup-expired-token")
		assertThat(manualExecution.taskCode).isEqualTo("cleanup-expired-token")
		assertThat(executions.map { it.payloadSnapshot["scope"] })
			.contains("expired-token", "manual")
		assertThat(service.getTask(task.id).lastExecutionStatus).isEqualTo("SUCCESS")
	}

	@Test
	fun `disabled managed task is not triggerable until enabled`() {
		val task = service.createTask(
			SaveManagedScheduledTaskCommand(
				code = "run-report-once",
				handlerCode = "test.echo",
				name = "Run Report Once",
				description = null,
				groupName = "system",
				scheduleType = "ONCE",
				cronExpression = null,
				intervalSeconds = null,
				runAt = Instant.now().plusSeconds(3600),
				timeZone = ZoneId.of("UTC"),
				payload = emptyMap(),
				enabled = false,
			),
		)

		assertThat(service.triggerNow(task.id)).isFalse()

		service.enableTask(task.id)
		assertThat(service.triggerNow(task.id)).isTrue()
		assertThat(SchedulerManagementTestHandler.latch.await(3, TimeUnit.SECONDS)).isTrue()
	}

	/**
	 * Quartz 在后台完成执行记录事务，因此测试需要等待目标 payload 对应的成功记录，不能依赖列表首项或 handler 已进入。
	 */
	private fun awaitSuccessfulExecution(taskId: Long, scope: String): ManagedScheduledTaskExecutionResponse {
		val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(EXECUTION_TIMEOUT_SECONDS)
		do {
			service.listExecutions(taskId, 0, 20).rows
				.firstOrNull { execution ->
					execution.status == "SUCCESS" && execution.payloadSnapshot["scope"] == scope
				}
				?.let { return it }
			Thread.sleep(EXECUTION_POLL_INTERVAL_MILLIS)
		} while (System.nanoTime() < deadline)
		throw AssertionError("Timed out waiting for successful scheduler execution with scope '$scope'")
	}

	private companion object {
		private const val EXECUTION_TIMEOUT_SECONDS = 5L
		private const val EXECUTION_POLL_INTERVAL_MILLIS = 25L
	}
}
