package io.github.lishangbu.system.scheduler

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 手动触发定时任务的请求。
 */
@Schema(description = "手动触发定时任务请求。payload 只影响本次触发，不会修改任务定义。")
data class TriggerScheduledTaskRequest(
	@field:Schema(description = "本次手动触发传给处理器的 JSON payload。为空时使用任务定义中的 payload。", example = "{\"scope\":\"manual\"}")
	var payload: Map<String, Any?> = emptyMap(),
)
