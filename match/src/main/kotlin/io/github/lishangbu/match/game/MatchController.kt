package io.github.lishangbu.match.game

import io.github.lishangbu.match.trainer.TrainerSessionService
import io.github.lishangbu.match.trainer.accountId
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

/** 当前 Match API 始终从 OAuth 与 Trainer Session 派生查看者身份。 */
@RestController
@RequestMapping("/api/player/matches")
class MatchController(private val matches: MatchService, private val sessions: TrainerSessionService) {
	@GetMapping("/current")
	fun current(authentication: Authentication, @RequestHeader("X-Trainer-Session") credential: String): MatchViewResponse =
		sessions.current(authentication.accountId(), credential).let {
			matches.current(it.session.accountId, it.session.trainerId)
		}

	@GetMapping("/{matchId}")
	fun find(
		authentication: Authentication,
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable matchId: String,
	): MatchViewResponse = sessions.current(authentication.accountId(), credential).let {
		matches.view(it.session.accountId, it.session.trainerId, matchId.toLongOrNull() ?: -1)
	}

	@PostMapping("/{matchId}/turns")
	fun submitTurn(
		authentication: Authentication,
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable matchId: String,
		@RequestBody request: SubmitMatchTurnRequest,
	): MatchTurnResponse = sessions.current(authentication.accountId(), credential).let {
		matches.submitTurn(it.session.accountId, it.session.trainerId, matchId.toLongOrNull() ?: -1, request)
	}

	@PostMapping("/{matchId}/forfeit")
	fun forfeit(
		authentication: Authentication,
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable matchId: String,
		@RequestBody request: ForfeitMatchRequest,
	): MatchViewResponse = sessions.current(authentication.accountId(), credential).let {
		matches.forfeit(it.session.accountId, it.session.trainerId, matchId.toLongOrNull() ?: -1, request.expectedRevision)
	}
}
