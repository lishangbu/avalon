package io.github.lishangbu.system.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/**
 * 管理端创建或更新定时任务的请求。
 */
@Schema(description = "创建或更新定时任务请求。scheduleType 决定使用哪组调度字段。")
data class ScheduledTaskRequestPayload(
	@field:Schema(description = "任务稳定 code。必须唯一。", example = "cleanup-expired-token")
	var code: String = "",
	@field:Schema(description = "任务处理器 code。必须对应已注册的 ScheduledTaskHandler。", example = "token.cleanup")
	var handlerCode: String = "",
	@field:Schema(description = "任务展示名称。", example = "清理过期 token")
	var name: String = "",
	@field:Schema(description = "任务说明。用于管理端展示和排查。", example = "定期清理过期授权状态", nullable = true)
	var description: String? = null,
	@field:Schema(description = "任务分组。用于调度器分组和管理端过滤。", example = "system")
	var groupName: String = "default",
	@field:Schema(description = "调度类型。CRON 使用 cronExpression，FIXED_INTERVAL 使用 intervalSeconds，ONCE 使用 runAt。", example = "CRON")
	var scheduleType: String = "",
	@field:Schema(description = "Cron 表达式。scheduleType=CRON 时必填。", example = "0 0/5 * * * ?", nullable = true)
	var cronExpression: String? = null,
	@field:Schema(description = "固定间隔秒数。scheduleType=FIXED_INTERVAL 时必填。", example = "300", nullable = true)
	var intervalSeconds: Long? = null,
	@field:Schema(description = "一次性执行时间。scheduleType=ONCE 时必填。", example = "2026-06-25T15:30:00Z", nullable = true)
	var runAt: Instant? = null,
	@field:Schema(description = "调度时区。Cron 调度会使用该时区解释表达式。", example = "UTC")
	var timeZone: String = "UTC",
	@field:Schema(description = "传给任务处理器的默认 JSON payload。", example = "{\"scope\":\"expired-token\"}")
	var payload: Map<String, Any?> = emptyMap(),
	@field:Schema(description = "任务创建或更新后是否启用。", example = "true")
	var enabled: Boolean = false,
)
