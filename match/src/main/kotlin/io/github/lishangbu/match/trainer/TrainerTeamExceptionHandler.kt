package io.github.lishangbu.match.trainer

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** 将 Team 与 Trainer Session 边界失败转换为稳定 JSON 错误契约。 */
@RestControllerAdvice(assignableTypes = [TrainerTeamController::class])
class TrainerTeamExceptionHandler {
	@ExceptionHandler(TrainerTeamRequestException::class)
	fun handleTeam(error: TrainerTeamRequestException): ResponseEntity<TrainerTeamErrorResponse> {
		val status = when (error.code) {
			"trainer-team.not-found" -> HttpStatus.NOT_FOUND
			"trainer-team.revision-conflict" -> HttpStatus.CONFLICT
			else -> HttpStatus.UNPROCESSABLE_ENTITY
		}
		return ResponseEntity.status(status).body(TrainerTeamErrorResponse(error.code, error.code))
	}

	@ExceptionHandler(TrainerSessionRequestException::class)
	fun handleSession(error: TrainerSessionRequestException): ResponseEntity<TrainerTeamErrorResponse> =
		ResponseEntity.status(error.status).body(TrainerTeamErrorResponse(error.code, error.code))
}
