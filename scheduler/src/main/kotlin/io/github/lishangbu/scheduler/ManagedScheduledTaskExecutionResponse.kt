package io.github.lishangbu.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import java.time.Instant

/**
 * 管理端定时任务执行记录响应。
 */
@Schema(description = "管理端定时任务执行记录响应。")
@Immutable
interface ManagedScheduledTaskExecutionResponse {
	@get:Schema(
		type = "string",
		description = "执行记录主键 ID。",
		example = "20001",
		nullable = false,
		requiredMode = Schema.RequiredMode.REQUIRED,
	)
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(
		type = "string",
		description = "所属定时任务主键 ID。",
		example = "10001",
		nullable = false,
		requiredMode = Schema.RequiredMode.REQUIRED,
	)
	@JsonConverter(LongToStringConverter::class)
	val taskId: Long
	val taskCode: String
	val handlerCode: String
	val scheduledFireTime: Instant?
	val actualFireTime: Instant
	val finishedAt: Instant?
	val status: String
	val durationMs: Long?
	val refireCount: Int
	val payloadSnapshot: Map<String, Any?>
	val errorMessage: String?
}
