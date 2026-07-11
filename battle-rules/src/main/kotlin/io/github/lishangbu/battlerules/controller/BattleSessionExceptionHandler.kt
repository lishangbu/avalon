package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.battlesession.BattleSessionNotFoundException
import io.github.lishangbu.battlesession.SessionRevisionConflictException
import io.github.lishangbu.battlesession.SessionCapacityExhaustedException
import io.github.lishangbu.battlesession.BattleSessionNotActiveException
import io.github.lishangbu.battlesession.CommandPayloadConflictException
import io.github.lishangbu.battlesession.IncompleteTurnCommandException
import io.github.lishangbu.battlesession.InvalidTurnActionsException
import io.github.lishangbu.common.web.ApiErrorCode
import io.github.lishangbu.common.web.ApiErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** 将 Session Runtime 异常限制在稳定的管理 API 错误协议内。 */
@RestControllerAdvice(assignableTypes = [BattleSessionController::class])
class BattleSessionExceptionHandler {
	@ExceptionHandler(IncompleteTurnCommandException::class)
	fun handleIncompleteTurn(): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(
				ApiErrorResponse(
					code = ApiErrorCode.VALIDATION_INVALID.value,
					message = "actions 必须恰好满足当前全部 Turn Requirements",
					field = "actions",
				),
			)

	@ExceptionHandler(InvalidTurnActionsException::class)
	fun handleInvalidTurnActions(exception: InvalidTurnActionsException): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(
				ApiErrorResponse(
					code = ApiErrorCode.VALIDATION_INVALID.value,
					message = "actions 包含非法行动组合: ${exception.violations.joinToString { it.code }}",
					field = "actions",
				),
			)

	@ExceptionHandler(CommandPayloadConflictException::class)
	fun handleCommandPayloadConflict(): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.status(HttpStatus.CONFLICT)
			.body(
				ApiErrorResponse(
					code = ApiErrorCode.RESOURCE_CONFLICT.value,
					message = "commandId 已被用于其他命令负载",
					field = "commandId",
				),
			)

	@ExceptionHandler(BattleSessionNotActiveException::class)
	fun handleNotActive(): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.status(HttpStatus.CONFLICT)
			.body(
				ApiErrorResponse(
					code = ApiErrorCode.RESOURCE_CONFLICT.value,
					message = "Battle Session 已进入终态，不能继续执行命令",
					field = "sessionId",
				),
			)

	@ExceptionHandler(SessionCapacityExhaustedException::class)
	fun handleCapacityExhausted(exception: SessionCapacityExhaustedException): ResponseEntity<ApiErrorResponse> {
		val retryAfterSeconds = ((exception.retryAfter.toMillis() + 999) / 1_000).coerceAtLeast(1)
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
			.header(HttpHeaders.RETRY_AFTER, retryAfterSeconds.toString())
			.body(
				ApiErrorResponse(
					code = exception.code,
					message = "Battle Session Runtime 容量已满，请稍后重试",
				),
			)
	}

	@ExceptionHandler(SessionRevisionConflictException::class)
	fun handleRevisionConflict(exception: SessionRevisionConflictException): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.status(HttpStatus.CONFLICT)
			.body(
				ApiErrorResponse(
					code = ApiErrorCode.RESOURCE_CONFLICT.value,
					message = "Session revision 已变化，当前 revision 为 ${exception.actualRevision}",
					field = "expectedRevision",
				),
			)

	@ExceptionHandler(BattleSessionNotFoundException::class)
	fun handleNotFound(): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(
				ApiErrorResponse(
					code = ApiErrorCode.RESOURCE_NOT_FOUND.value,
					message = "战斗会话不存在或已被淘汰",
					field = "sessionId",
				),
			)
}
