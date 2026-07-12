package io.github.lishangbu.match.challenge

import io.github.lishangbu.match.trainer.InvalidTrainerSessionException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import io.github.lishangbu.match.game.MatchStartException
import io.github.lishangbu.match.game.MatchController
import org.springframework.web.bind.annotation.RestControllerAdvice

/** 将 Challenge 领域失败映射为明确的 HTTP 状态与稳定 code。 */
@RestControllerAdvice(assignableTypes = [ChallengeController::class, MatchController::class])
class ChallengeExceptionHandler {
	@ExceptionHandler(MatchStartException::class)
	fun startFailed(error: MatchStartException) = ResponseEntity.status(503)
		.body(ChallengeErrorResponse { code = "match.start-failed"; message = code; matchId = error.matchId })

	@ExceptionHandler(ChallengeRequestException::class)
	fun handle(error: ChallengeRequestException) =
		ResponseEntity.status(error.status).body(ChallengeErrorResponse { code = error.code; message = code; matchId = null })

	@ExceptionHandler(InvalidTrainerSessionException::class)
	fun invalidSession() = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
		.body(ChallengeErrorResponse { code = "trainer-session.invalid"; message = "Trainer Session 无效或已过期"; matchId = null })
}
