package io.github.lishangbu.match.challenge

import io.github.lishangbu.match.trainer.TrainerSessionService
import io.github.lishangbu.match.trainer.accountId
import io.github.lishangbu.match.game.AcceptChallengeRequest
import io.github.lishangbu.match.game.MatchResponse
import io.github.lishangbu.match.game.MatchService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema

/** Challenge REST 边界只从 OAuth 与 Trainer Session 派生当前 Trainer。 */
@RestController
@RequestMapping("/api/player/challenges")
class ChallengeController(
	private val service: ChallengeService,
	private val sessions: TrainerSessionService,
	private val matches: MatchService,
) {
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	fun create(
		authentication: Authentication,
		@RequestHeader("X-Trainer-Session") credential: String,
		@RequestBody request: CreateChallengeRequest,
	): ChallengeResponse {
		val current = sessions.current(authentication.accountId(), credential)
		return service.create(current.session.accountId, current.session.trainerId, request)
	}

	@GetMapping
	fun list(authentication: Authentication, @RequestHeader("X-Trainer-Session") credential: String): List<ChallengeResponse> =
		sessions.current(authentication.accountId(), credential).let { service.list(it.session.trainerId) }

	@GetMapping("/{challengeId}")
	fun find(
		authentication: Authentication,
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable challengeId: String,
	): ChallengeResponse = sessions.current(authentication.accountId(), credential).let {
		service.find(it.session.trainerId, challengeId.toLongOrNull() ?: throw notFound())
	}

	@PostMapping("/{challengeId}/reject")
	fun reject(
		authentication: Authentication,
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable challengeId: String,
		@RequestBody request: ChallengeRevisionRequest,
	): ChallengeResponse = sessions.current(authentication.accountId(), credential).let {
		service.reject(it.session.trainerId, challengeId.toLongOrNull() ?: throw notFound(), request.expectedRevision)
	}

	@PostMapping("/{challengeId}/accept")
	@ApiResponses(value = [
		ApiResponse(responseCode = "200", description = "Match 已启动"),
		ApiResponse(
			responseCode = "503",
			description = "Match 已持久化，但 Battle Runtime 启动失败",
			content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = ChallengeErrorResponse::class))],
		),
	])
	fun accept(
		authentication: Authentication,
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable challengeId: String,
		@RequestBody request: AcceptChallengeRequest,
	): MatchResponse = sessions.current(authentication.accountId(), credential).let {
		matches.accept(
			it.session.accountId,
			it.session.trainerId,
			challengeId.toLongOrNull() ?: throw notFound(),
			request,
		)
	}

	@PostMapping("/{challengeId}/withdraw")
	fun withdraw(
		authentication: Authentication,
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable challengeId: String,
		@RequestBody request: ChallengeRevisionRequest,
	): ChallengeResponse = sessions.current(authentication.accountId(), credential).let {
		service.withdraw(it.session.trainerId, challengeId.toLongOrNull() ?: throw notFound(), request.expectedRevision)
	}

	private fun notFound() = ChallengeRequestException(HttpStatus.NOT_FOUND, "challenge.not-found")
}
