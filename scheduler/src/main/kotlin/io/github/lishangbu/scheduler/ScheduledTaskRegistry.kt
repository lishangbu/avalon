package io.github.lishangbu.scheduler

/**
 * 保存应用内所有可调度任务处理器。
 *
 * 注册表在启动时校验 code 唯一性，避免 Quartz 运行时才发现任务路由歧义。
 */
class ScheduledTaskRegistry(
	handlers: Collection<ScheduledTaskHandler>,
) {
	private val handlersByCode: Map<String, ScheduledTaskHandler> = handlers
		.map { handler -> handler.code.trim() to handler }
		.also { entries ->
			entries.filter { (code) -> code.isBlank() }
				.takeIf { it.isNotEmpty() }
				?.let {
					throw ScheduledTaskConfigurationException("定时任务处理器 code 不能为空")
				}
			entries.groupBy { (code) -> code }
				.filterValues { it.size > 1 }
				.keys
				.takeIf { it.isNotEmpty() }
				?.let { duplicatedCodes ->
					throw ScheduledTaskConfigurationException("定时任务处理器 code 重复: ${duplicatedCodes.joinToString()}")
				}
		}
		.associate { (code, handler) -> code to handler }

	val codes: Set<String>
		get() = handlersByCode.keys

	fun requireHandler(taskCode: String): ScheduledTaskHandler {
		val normalizedCode = taskCode.trim()
		return handlersByCode[normalizedCode] ?: throw ScheduledTaskNotFoundException(normalizedCode)
	}
}
