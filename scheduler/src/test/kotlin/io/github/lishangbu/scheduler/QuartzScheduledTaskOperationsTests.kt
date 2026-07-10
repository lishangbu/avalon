package io.github.lishangbu.scheduler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.quartz.Job
import org.quartz.JobBuilder
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.quartz.TriggerKey
import org.quartz.impl.StdSchedulerFactory
import org.quartz.spi.JobFactory
import org.quartz.spi.TriggerFiredBundle
import org.springframework.scheduling.quartz.SchedulerFactoryBean
import java.time.Duration
import java.time.Instant
import java.util.Properties
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import tools.jackson.databind.json.JsonMapper

class QuartzScheduledTaskOperationsTests {
	private val objectMapper = JsonMapper.builder().build()
	private val scheduler = newScheduler()

	@AfterEach
	fun shutdownScheduler() {
		scheduler.shutdown(true)
	}

	@Test
	fun `job factory keeps regular Quartz jobs executable`() {
		val latch = CountDownLatch(1)
		PlainQuartzJob.latch = latch
		scheduler.setJobFactory(
			DelegatingScheduledTaskJobFactory(
				ScheduledTaskRegistry(emptyList()),
				objectMapper,
				NoopScheduledTaskExecutionRecorder(),
			),
		)
		scheduler.start()

		scheduler.scheduleJob(
			JobBuilder.newJob(PlainQuartzJob::class.java)
				.withIdentity("plain-job", "default")
				.build(),
			TriggerBuilder.newTrigger()
				.withIdentity("plain-job", "default")
				.startNow()
				.build(),
		)

		assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
	}

	@Test
	fun `schedule stores Quartz job and triggerNow delegates payload to registered handler`() {
		val latch = CountDownLatch(1)
		val executions = mutableListOf<ScheduledTaskExecution>()
		val registry = ScheduledTaskRegistry(
			listOf(
				object : ScheduledTaskHandler {
					override val code: String = "system.cleanup"

					override fun execute(execution: ScheduledTaskExecution) {
						executions += execution
						latch.countDown()
					}
				},
			),
		)
		scheduler.setJobFactory(StaticDelegatingJobFactory(registry, objectMapper))
		scheduler.start()
		val operations = QuartzScheduledTaskOperations(scheduler, registry, objectMapper)

		val ref = operations.schedule(
			ScheduledTaskRequest(
				taskId = "cleanup-daily",
				taskCode = "system.cleanup",
				schedule = ScheduledTaskSchedule.Once(Instant.now().plusSeconds(60)),
				payload = mapOf("scope" to "expired-token"),
			),
		)
		operations.triggerNow(ref, mapOf("scope" to "manual"))

		assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
		assertThat(scheduler.checkExists(JobKey.jobKey("cleanup-daily", "default"))).isTrue()
		assertThat(executions.single().taskId).isEqualTo("cleanup-daily")
		assertThat(executions.single().taskCode).isEqualTo("system.cleanup")
		assertThat(executions.single().payload).containsEntry("scope", "manual")
	}

	@Test
	fun `pause resume and delete update Quartz job lifecycle`() {
		val registry = ScheduledTaskRegistry(
			listOf(
				object : ScheduledTaskHandler {
					override val code: String = "system.cleanup"

					override fun execute(execution: ScheduledTaskExecution) = Unit
				},
			),
		)
		val operations = QuartzScheduledTaskOperations(scheduler, registry, objectMapper)
		scheduler.start()

		val ref = operations.schedule(
			ScheduledTaskRequest(
				taskId = "cleanup-hourly",
				taskCode = "system.cleanup",
				schedule = ScheduledTaskSchedule.FixedInterval(
					interval = Duration.ofHours(1),
					startAt = Instant.now().plusSeconds(60),
				),
			),
		)

		operations.pause(ref)
		assertThat(scheduler.getTriggerState(TriggerKey.triggerKey("cleanup-hourly", "default")))
			.isEqualTo(Trigger.TriggerState.PAUSED)

		operations.resume(ref)
		assertThat(scheduler.getTriggerState(TriggerKey.triggerKey("cleanup-hourly", "default")))
			.isEqualTo(Trigger.TriggerState.NORMAL)

		assertThat(operations.delete(ref)).isTrue()
		assertThat(operations.delete(ref)).isFalse()
	}

	@Test
	fun `schedule rejects unknown task code before creating Quartz job`() {
		val registry = ScheduledTaskRegistry(emptyList())
		val operations = QuartzScheduledTaskOperations(scheduler, registry, objectMapper)

		assertThrows<ScheduledTaskNotFoundException> {
			operations.schedule(
				ScheduledTaskRequest(
					taskId = "missing-task",
					taskCode = "missing",
					schedule = ScheduledTaskSchedule.Once(Instant.now()),
				),
			)
		}
		assertThat(scheduler.checkExists(JobKey.jobKey("missing-task", "default"))).isFalse()
	}

	private fun newScheduler(): Scheduler {
		val factory = StdSchedulerFactory()
		factory.initialize(
			Properties().apply {
				setProperty("org.quartz.scheduler.instanceName", "scheduler-test-${System.nanoTime()}")
				setProperty("org.quartz.scheduler.skipUpdateCheck", "true")
				setProperty("org.quartz.threadPool.threadCount", "2")
				setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore")
			},
		)
		return factory.scheduler
	}

	private class StaticDelegatingJobFactory(
		private val registry: ScheduledTaskRegistry,
		private val objectMapper: tools.jackson.databind.ObjectMapper,
	) : JobFactory {
		override fun newJob(bundle: TriggerFiredBundle, scheduler: Scheduler): Job =
			DelegatingScheduledTaskJob(registry, objectMapper)
	}

	private class PlainQuartzJob : Job {
		override fun execute(context: org.quartz.JobExecutionContext) {
			latch.countDown()
		}

		companion object {
			lateinit var latch: CountDownLatch
		}
	}
}
