package io.github.lishangbu.match.challenge

import io.github.lishangbu.common.web.pathIdentifier
import io.github.lishangbu.match.trainer.TrainerSessionService
import io.github.lishangbu.match.trainer.currentAccountId
import io.github.lishangbu.match.game.AcceptChallengeRequest
import io.github.lishangbu.match.game.MatchResponse
import io.github.lishangbu.match.game.MatchService
import io.github.lishangbu.match.event.PlayerEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema

/** Challenge REST 边界只从登录账户与 Trainer Session 派生当前 Trainer。 */
@RestController
@RequestMapping("/api/player/challenges")
class ChallengeController(
	private val service: ChallengeService,
	private val sessions: TrainerSessionService,
	private val matches: MatchService,
	private val events: PlayerEventPublisher,
) {
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	fun create(
		@RequestHeader("X-Trainer-Session") credential: String,
		@RequestBody request: CreateChallengeRequest,
	): ChallengeResponse {
		val current = sessions.current(currentAccountId(), credential)
		return service.create(current.session.accountId, current.session.trainerId, request).also {
			events.challengeChanged(it.id, it.revision)
		}
	}

	@GetMapping
	fun list(@RequestHeader("X-Trainer-Session") credential: String): List<ChallengeResponse> =
		sessions.current(currentAccountId(), credential).let { service.list(it.session.trainerId) }

	@GetMapping("/{challengeId}")
	fun find(
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable challengeId: String,
	): ChallengeResponse = sessions.current(currentAccountId(), credential).let {
		service.find(it.session.trainerId, challengeId.pathIdentifier("challengeId"))
	}

	@PostMapping("/{challengeId}/reject")
	fun reject(
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable challengeId: String,
		@RequestBody request: ChallengeRevisionRequest,
	): ChallengeResponse = sessions.current(currentAccountId(), credential).let {
		service.reject(it.session.trainerId, challengeId.pathIdentifier("challengeId"), request.expectedRevision).also { changed ->
			events.challengeChanged(changed.id, changed.revision)
		}
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
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable challengeId: String,
		@RequestBody request: AcceptChallengeRequest,
	): MatchResponse = sessions.current(currentAccountId(), credential).let {
		matches.accept(
			it.session.accountId,
			it.session.trainerId,
			challengeId.pathIdentifier("challengeId"),
			request,
		).also { match ->
			events.challengeChanged(challengeId.toLong(), request.expectedRevision + 1)
			events.matchChanged(match.id, match.revision)
		}
	}

	@PostMapping("/{challengeId}/withdraw")
	fun withdraw(
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable challengeId: String,
		@RequestBody request: ChallengeRevisionRequest,
	): ChallengeResponse = sessions.current(currentAccountId(), credential).let {
		service.withdraw(it.session.trainerId, challengeId.pathIdentifier("challengeId"), request.expectedRevision).also { changed ->
			events.challengeChanged(changed.id, changed.revision)
		}
	}

}
