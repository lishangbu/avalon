package io.github.lishangbu.system.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 手动触发定时任务的响应。
 */
@Schema(description = "手动触发定时任务响应。triggered=true 表示触发请求已提交给调度器。")
data class TriggerScheduledTaskResponse(
	@field:Schema(description = "触发请求是否已提交。true 不代表任务业务逻辑已经执行完成。", example = "true")
	val triggered: Boolean,
)
