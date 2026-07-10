package io.github.lishangbu.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import java.time.Instant

/**
 * 管理端定时任务定义响应。
 */
@Schema(description = "管理端定时任务定义响应。")
@Immutable
interface ManagedScheduledTaskResponse {
	@get:Schema(
		type = "string",
		description = "定时任务主键 ID。",
		example = "10001",
		nullable = false,
		requiredMode = Schema.RequiredMode.REQUIRED,
	)
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	val code: String
	val handlerCode: String
	val name: String
	val description: String?
	val groupName: String
	val scheduleType: String
	val cronExpression: String?
	val intervalSeconds: Long?
	val runAt: Instant?
	val timeZone: String
	val payload: Map<String, Any?>
	val enabled: Boolean
	val nextFireTime: Instant?
	val triggerState: String?
	val lastExecutionStatus: String?
	val lastExecutionAt: Instant?
}
