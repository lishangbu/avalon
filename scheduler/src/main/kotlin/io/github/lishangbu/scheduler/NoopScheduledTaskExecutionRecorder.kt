package io.github.lishangbu.scheduler

internal class NoopScheduledTaskExecutionRecorder : ScheduledTaskExecutionRecorder {
	override fun record(execution: ScheduledTaskExecution, executeHandler: () -> Unit) {
		executeHandler()
	}
}
