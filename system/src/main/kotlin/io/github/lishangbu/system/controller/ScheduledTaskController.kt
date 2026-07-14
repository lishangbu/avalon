package io.github.lishangbu.system.controller

import io.github.lishangbu.common.web.security.RequireSecurityAdmin

import io.github.lishangbu.system.dto.ScheduledTaskRequestPayload
import io.github.lishangbu.system.dto.TriggerScheduledTaskRequest
import io.github.lishangbu.system.dto.TriggerScheduledTaskResponse
import io.github.lishangbu.system.service.ScheduledTaskSystemService
import io.github.lishangbu.scheduler.ManagedScheduledTaskExecutionResponse
import io.github.lishangbu.scheduler.ManagedScheduledTaskResponse
import io.github.lishangbu.scheduler.ScheduledTaskManagementService
import io.github.lishangbu.common.web.ApiErrorResponse
import io.github.lishangbu.system.openapi.SYSTEM_API_BAD_REQUEST_DESCRIPTION
import io.github.lishangbu.system.openapi.SYSTEM_API_BEARER_AUTH
import io.github.lishangbu.system.openapi.SYSTEM_API_CONFLICT_DESCRIPTION
import io.github.lishangbu.system.openapi.SYSTEM_API_FORBIDDEN_DESCRIPTION
import io.github.lishangbu.system.openapi.SYSTEM_API_NOT_FOUND_DESCRIPTION
import io.github.lishangbu.system.openapi.SYSTEM_API_UNAUTHORIZED_DESCRIPTION
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody as OpenApiRequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.babyfish.jimmer.Page
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 定时任务系统管理 API。
 */
@RequireSecurityAdmin
@RestController
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@RequestMapping("/api/system/scheduler/tasks")
@Tag(
	name = "定时任务",
	description = "管理系统内可持久化的定时任务，包括任务定义、启停状态、手动触发和执行记录查询。",
)
class ScheduledTaskController(
	private val service: ScheduledTaskSystemService,
) {
	@GetMapping
	@Operation(
		summary = "查询定时任务列表",
		description = "分页查询定时任务定义和最近执行状态。q 会匹配任务 code、名称、handlerCode 或分组名。",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "定时任务列表读取成功。"),
			ApiResponse(responseCode = "400", description = SYSTEM_API_BAD_REQUEST_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun listTasks(
		@Parameter(description = "模糊搜索关键字，匹配 code、name、handlerCode 或 groupName。", example = "cleanup")
		@RequestParam(required = false) q: String?,
		@Parameter(description = "页码，从 0 开始。", example = "0")
		@RequestParam(defaultValue = "0") page: Int,
		@Parameter(description = "每页数量，最大 100。", example = "50")
		@RequestParam(defaultValue = "50") size: Int,
	): Page<ManagedScheduledTaskResponse> =
		service.listTasks(page, size, q)

	@GetMapping("/{taskId}")
	@Operation(
		summary = "查询定时任务详情",
		description = "按任务主键查询定时任务定义、调度表达式、启用状态和最近执行快照。",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "定时任务详情读取成功。", content = [Content(schema = Schema(implementation = ManagedScheduledTaskResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun getTask(
		@Parameter(description = "定时任务主键 ID。", example = "90001")
		@PathVariable taskId: Long,
	): ManagedScheduledTaskResponse =
		service.getTask(taskId)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(
		summary = "创建定时任务",
		description = """
			创建可管理的定时任务。scheduleType 决定使用 cronExpression、intervalSeconds 或 runAt 中的哪类调度字段。

			handlerCode 必须对应已注册的任务处理器；payload 是传给处理器的 JSON 对象，会在执行记录中保留快照。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "201", description = "定时任务创建成功。", content = [Content(schema = Schema(implementation = ManagedScheduledTaskResponse::class))]),
			ApiResponse(responseCode = "400", description = SYSTEM_API_BAD_REQUEST_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "409", description = SYSTEM_API_CONFLICT_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun createTask(
		@OpenApiRequestBody(
			description = "定时任务创建请求。不同 scheduleType 对应不同调度字段组合。",
			required = true,
			content = [Content(schema = Schema(implementation = ScheduledTaskRequestPayload::class))],
		)
		@RequestBody request: ScheduledTaskRequestPayload,
	): ManagedScheduledTaskResponse =
		service.createTask(request)

	@PutMapping("/{taskId}")
	@Operation(
		summary = "更新定时任务",
		description = """
			更新定时任务定义并重新应用调度。请求体是更新后的完整任务定义，不是局部补丁。

			如果更新调度表达式或 enabled 状态，服务端会同步调整 Quartz 任务注册状态。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "定时任务更新成功。", content = [Content(schema = Schema(implementation = ManagedScheduledTaskResponse::class))]),
			ApiResponse(responseCode = "400", description = SYSTEM_API_BAD_REQUEST_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun updateTask(
		@Parameter(description = "定时任务主键 ID。", example = "90001")
		@PathVariable taskId: Long,
		@OpenApiRequestBody(
			description = "定时任务更新请求。会整体覆盖任务可管理字段。",
			required = true,
			content = [Content(schema = Schema(implementation = ScheduledTaskRequestPayload::class))],
		)
		@RequestBody request: ScheduledTaskRequestPayload,
	): ManagedScheduledTaskResponse =
		service.updateTask(taskId, request)

	@PostMapping("/{taskId}/enable")
	@Operation(
		summary = "启用定时任务",
		description = "启用定时任务并在调度器中注册对应触发器。启用后任务会按自身调度表达式自动执行。",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "定时任务已启用。", content = [Content(schema = Schema(implementation = ManagedScheduledTaskResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun enableTask(
		@Parameter(description = "定时任务主键 ID。", example = "90001")
		@PathVariable taskId: Long,
	): ManagedScheduledTaskResponse =
		service.enableTask(taskId)

	@PostMapping("/{taskId}/disable")
	@Operation(
		summary = "禁用定时任务",
		description = "禁用定时任务并从调度器中移除自动触发器。已产生的执行记录不会删除。",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "定时任务已禁用。", content = [Content(schema = Schema(implementation = ManagedScheduledTaskResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun disableTask(
		@Parameter(description = "定时任务主键 ID。", example = "90001")
		@PathVariable taskId: Long,
	): ManagedScheduledTaskResponse =
		service.disableTask(taskId)

	@PostMapping("/{taskId}/trigger")
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(
		summary = "手动触发定时任务",
		description = """
			立即触发一次定时任务执行。手动触发不会改变任务的自动调度表达式。

			请求体可以传入本次触发专用 payload；未传入时使用任务定义中的默认 payload。
			接口返回 202 表示触发请求已提交到调度器，不代表任务业务逻辑已经执行完成。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "202", description = "已接受触发请求", content = [Content(schema = Schema(implementation = TriggerScheduledTaskResponse::class))]),
			ApiResponse(responseCode = "400", description = SYSTEM_API_BAD_REQUEST_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun triggerTask(
		@Parameter(description = "定时任务主键 ID。", example = "90001")
		@PathVariable taskId: Long,
		@OpenApiRequestBody(
			description = "可选的手动触发 payload。省略时使用任务定义中的 payload。",
			required = false,
			content = [Content(schema = Schema(implementation = TriggerScheduledTaskRequest::class))],
		)
		@RequestBody request: TriggerScheduledTaskRequest?,
	): TriggerScheduledTaskResponse =
		service.triggerTask(taskId, request ?: TriggerScheduledTaskRequest())

	@GetMapping("/{taskId}/executions")
	@Operation(
		summary = "查询定时任务执行记录",
		description = "分页查询指定任务的执行记录，包含执行状态、开始/结束时间、payload 快照和失败消息。",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "执行记录列表读取成功。"),
			ApiResponse(responseCode = "400", description = SYSTEM_API_BAD_REQUEST_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun listExecutions(
		@Parameter(description = "定时任务主键 ID。", example = "90001")
		@PathVariable taskId: Long,
		@Parameter(description = "页码，从 0 开始。", example = "0")
		@RequestParam(defaultValue = "0") page: Int,
		@Parameter(description = "每页数量，最大 100。", example = "50")
		@RequestParam(defaultValue = "50") size: Int,
	): Page<ManagedScheduledTaskExecutionResponse> =
		service.listExecutions(taskId, page, size)

	@DeleteMapping("/{taskId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(
		summary = "删除定时任务",
		description = "删除定时任务定义，并从调度器中移除对应触发器。删除成功后不返回响应体。",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "204", description = "定时任务已删除。"),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun deleteTask(
		@Parameter(description = "定时任务主键 ID。", example = "90001")
		@PathVariable taskId: Long,
	) {
		service.deleteTask(taskId)
	}
}
