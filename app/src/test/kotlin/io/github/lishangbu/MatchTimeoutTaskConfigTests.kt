package io.github.lishangbu

import io.github.lishangbu.scheduler.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.beans.factory.support.StaticListableBeanFactory

/** 验证 Match 超时任务的稳定标识与调度周期；使用内存替身隔离真实 Quartz Scheduler。 */
class MatchTimeoutTaskConfigTests {
	@Test
	fun `registers one stable Quartz fixed interval task`() {
		val operations = RecordingOperations()
		val provider = StaticListableBeanFactory(mapOf("operations" to operations))
			.getBeanProvider(ScheduledTaskOperations::class.java)
		MatchTimeoutTaskConfig().matchTimeoutTaskRegistration(provider)
			.run(DefaultApplicationArguments())

		val request = operations.request
		assertThat(request.taskId).isEqualTo("match-turn-timeout")
		assertThat(request.taskCode).isEqualTo("match-turn-timeout")
		assertThat(request.group).isEqualTo("match-runtime")
		assertThat((request.schedule as ScheduledTaskSchedule.FixedInterval).interval.seconds).isEqualTo(1)
	}

	private class RecordingOperations : ScheduledTaskOperations {
		lateinit var request: ScheduledTaskRequest
		override fun schedule(request: ScheduledTaskRequest): ScheduledTaskReference {
			this.request = request
			return ScheduledTaskReference(request.taskId, request.group)
		}
		override fun triggerNow(reference: ScheduledTaskReference, payload: Map<String, Any?>) = false
		override fun pause(reference: ScheduledTaskReference) = false
		override fun resume(reference: ScheduledTaskReference) = false
		override fun delete(reference: ScheduledTaskReference) = false
		override fun exists(reference: ScheduledTaskReference) = false
	}
}
