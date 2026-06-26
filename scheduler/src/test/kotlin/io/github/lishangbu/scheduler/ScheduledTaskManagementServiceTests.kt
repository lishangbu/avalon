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
				cronExpression = "0 0/5 * * * ?",
				intervalSeconds = null,
				runAt = null,
				timeZone = ZoneId.of("UTC"),
				payload = mapOf("scope" to "expired-token"),
				enabled = true,
			),
		)

		val triggered = service.triggerNow(task.id, mapOf("scope" to "manual"))

		assertThat(triggered).isTrue()
		assertThat(SchedulerManagementTestHandler.latch.await(3, TimeUnit.SECONDS)).isTrue()
		val executions = service.listExecutions(task.id, 0, 10).rows
		assertThat(executions).hasSize(1)
		assertThat(executions.single().status).isEqualTo("SUCCESS")
		assertThat(executions.single().taskCode).isEqualTo("cleanup-expired-token")
		assertThat(executions.single().payloadSnapshot).containsEntry("scope", "manual")
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
}
