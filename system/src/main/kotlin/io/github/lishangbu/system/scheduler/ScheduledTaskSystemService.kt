package io.github.lishangbu.system.scheduler

import io.github.lishangbu.scheduler.ManagedScheduledTaskConflictException
import io.github.lishangbu.scheduler.ManagedScheduledTaskExecutionResponse
import io.github.lishangbu.scheduler.ManagedScheduledTaskNotFoundException
import io.github.lishangbu.scheduler.ManagedScheduledTaskResponse
import io.github.lishangbu.scheduler.ManagedScheduledTaskValidationException
import io.github.lishangbu.scheduler.SaveManagedScheduledTaskCommand
import io.github.lishangbu.scheduler.ScheduledTaskManagementService
import io.github.lishangbu.scheduler.ScheduledTaskNotFoundException
import io.github.lishangbu.common.web.ApiErrorCode
import io.github.lishangbu.common.web.ApiException
import org.babyfish.jimmer.Page
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.ZoneId

/**
 * 定时任务管理端协议服务。
 *
 * 该服务只负责把 HTTP 请求转换为 scheduler 模块命令，并复用系统管理 API 的稳定错误模型。
 */
@Service
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class ScheduledTaskSystemService(
	private val taskManagementService: ScheduledTaskManagementService,
) {
	fun listTasks(page: Int, size: Int, query: String?): Page<ManagedScheduledTaskResponse> =
		wrapSchedulerErrors {
			taskManagementService.listTasks(page, size, query)
		}

	fun getTask(taskId: Long): ManagedScheduledTaskResponse =
		wrapSchedulerErrors {
			taskManagementService.getTask(taskId)
		}

	fun createTask(request: ScheduledTaskRequestPayload): ManagedScheduledTaskResponse =
		wrapSchedulerErrors {
			taskManagementService.createTask(request.toCommand())
		}

	fun updateTask(taskId: Long, request: ScheduledTaskRequestPayload): ManagedScheduledTaskResponse =
		wrapSchedulerErrors {
			taskManagementService.updateTask(taskId, request.toCommand())
		}

	fun enableTask(taskId: Long): ManagedScheduledTaskResponse =
		wrapSchedulerErrors {
			taskManagementService.enableTask(taskId)
		}

	fun disableTask(taskId: Long): ManagedScheduledTaskResponse =
		wrapSchedulerErrors {
			taskManagementService.disableTask(taskId)
		}

	fun triggerTask(taskId: Long, request: TriggerScheduledTaskRequest): TriggerScheduledTaskResponse =
		wrapSchedulerErrors {
			TriggerScheduledTaskResponse(taskManagementService.triggerNow(taskId, request.payload))
		}

	fun listExecutions(taskId: Long, page: Int, size: Int): Page<ManagedScheduledTaskExecutionResponse> =
		wrapSchedulerErrors {
			taskManagementService.listExecutions(taskId, page, size)
		}

	fun deleteTask(taskId: Long) {
		wrapSchedulerErrors {
			taskManagementService.deleteTask(taskId)
		}
	}

	private fun ScheduledTaskRequestPayload.toCommand(): SaveManagedScheduledTaskCommand =
		SaveManagedScheduledTaskCommand(
			code = code,
			handlerCode = handlerCode,
			name = name,
			description = description,
			groupName = groupName,
			scheduleType = scheduleType,
			cronExpression = cronExpression,
			intervalSeconds = intervalSeconds,
			runAt = runAt,
			timeZone = parseZoneId(timeZone),
			payload = payload,
			enabled = enabled,
		)

	private fun parseZoneId(value: String): ZoneId =
		try {
			ZoneId.of(value.ifBlank { "UTC" })
		} catch (ex: RuntimeException) {
			throw ApiException(
				status = HttpStatus.BAD_REQUEST,
				code = ApiErrorCode.VALIDATION_INVALID,
				message = "timeZone 不支持: $value",
				field = "timeZone",
			)
		}

	private fun <T> wrapSchedulerErrors(action: () -> T): T =
		try {
			action()
		} catch (ex: ManagedScheduledTaskValidationException) {
			throw ApiException(
				status = HttpStatus.BAD_REQUEST,
				code = ApiErrorCode.VALIDATION_INVALID,
				message = ex.message.orEmpty(),
				field = ex.field,
			)
		} catch (ex: ManagedScheduledTaskConflictException) {
			throw ApiException(
				status = HttpStatus.CONFLICT,
				code = ApiErrorCode.RESOURCE_CONFLICT,
				message = ex.message.orEmpty(),
				field = "code",
			)
		} catch (ex: ManagedScheduledTaskNotFoundException) {
			throw ApiException(
				status = HttpStatus.NOT_FOUND,
				code = ApiErrorCode.RESOURCE_NOT_FOUND,
				message = ex.message.orEmpty(),
				field = "taskId",
			)
		} catch (ex: ScheduledTaskNotFoundException) {
			throw ApiException(
				status = HttpStatus.BAD_REQUEST,
				code = ApiErrorCode.VALIDATION_INVALID,
				message = ex.message.orEmpty(),
				field = "handlerCode",
			)
		}
}
