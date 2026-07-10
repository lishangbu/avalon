package io.github.lishangbu.scheduler

import tools.jackson.databind.ObjectMapper
import org.quartz.spi.TriggerFiredBundle
import org.springframework.scheduling.quartz.SpringBeanJobFactory

internal class DelegatingScheduledTaskJobFactory(
	private val registry: ScheduledTaskRegistry,
	private val objectMapper: ObjectMapper,
	private val executionRecorder: ScheduledTaskExecutionRecorder,
) : SpringBeanJobFactory() {
	override fun createJobInstance(bundle: TriggerFiredBundle): Any =
		if (bundle.jobDetail.jobClass == DelegatingScheduledTaskJob::class.java) {
			DelegatingScheduledTaskJob(registry, objectMapper, executionRecorder)
		} else {
			super.createJobInstance(bundle)
		}
}
