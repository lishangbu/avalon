package io.github.lishangbu.match.challenge

import io.github.lishangbu.match.trainer.InvalidTrainerSessionException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** 将 Challenge 领域失败映射为明确的 HTTP 状态与稳定 code。 */
@RestControllerAdvice(assignableTypes = [ChallengeController::class])
class ChallengeExceptionHandler {
	@ExceptionHandler(ChallengeRequestException::class)
	fun handle(error: ChallengeRequestException) =
		ResponseEntity.status(error.status).body(ChallengeErrorResponse(error.code, error.code))

	@ExceptionHandler(InvalidTrainerSessionException::class)
	fun invalidSession() = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
		.body(ChallengeErrorResponse("trainer-session.invalid", "Trainer Session 无效或已过期"))
}
