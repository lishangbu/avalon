package io.github.lishangbu.match.trainer

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** 将 Trainer Session 边界异常转换成稳定 JSON 错误契约。 */
@RestControllerAdvice(assignableTypes = [TrainerSessionController::class])
class TrainerSessionExceptionHandler {
	@ExceptionHandler(TrainerSessionRequestException::class)
	fun handle(error: TrainerSessionRequestException): ResponseEntity<TrainerSessionErrorResponse> =
		ResponseEntity.status(error.status).body(TrainerSessionErrorResponse(error.code, error.code))
}
