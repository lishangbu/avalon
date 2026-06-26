package io.github.lishangbu.scheduler

/**
 * 定时任务调度的应用侧入口。
 *
 * 该接口屏蔽 Quartz 的 JobKey、TriggerKey 和 JobDataMap 细节，供业务服务注册、
 * 暂停、恢复、删除和手动触发任务。
 */
interface ScheduledTaskOperations {
	fun schedule(request: ScheduledTaskRequest): ScheduledTaskReference

	fun triggerNow(
		reference: ScheduledTaskReference,
		payload: Map<String, Any?> = emptyMap(),
	): Boolean

	fun pause(reference: ScheduledTaskReference): Boolean

	fun resume(reference: ScheduledTaskReference): Boolean

	fun delete(reference: ScheduledTaskReference): Boolean

	fun exists(reference: ScheduledTaskReference): Boolean
}
