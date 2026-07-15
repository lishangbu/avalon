package io.github.lishangbu.match.game

import io.github.lishangbu.common.web.pathIdentifier
import io.github.lishangbu.common.web.queryCursorIdentifier
import io.github.lishangbu.match.trainer.TrainerSessionService
import io.github.lishangbu.match.trainer.currentAccountId
import io.github.lishangbu.match.event.PlayerEventPublisher
import org.springframework.web.bind.annotation.*

/** 当前 Match API 始终从登录账户与 Trainer Session 派生查看者身份。 */
@RestController
@RequestMapping("/api/player/matches")
class MatchController(
	private val matches: MatchService,
	private val sessions: TrainerSessionService,
	private val events: PlayerEventPublisher,
) {
	@GetMapping("/history")
	fun history(
		@RequestHeader("X-Trainer-Session") credential: String,
		@RequestParam(required = false) beforeMatchId: String?,
		@RequestParam(defaultValue = "20") limit: Int,
	): List<MatchHistoryResponse> =
		sessions.current(currentAccountId(), credential).let {
			matches.history(it.session.accountId, it.session.trainerId, beforeMatchId = beforeMatchId.queryCursorIdentifier("beforeMatchId"), limit = limit)
		}

	@GetMapping("/history/{matchId}")
	fun historyDetail(
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable matchId: String,
	): MatchViewResponse = sessions.current(currentAccountId(), credential).let {
		matches.historyDetail(it.session.accountId, it.session.trainerId, matchId.pathIdentifier("matchId"))
	}

	@GetMapping("/current")
	fun current(@RequestHeader("X-Trainer-Session") credential: String): MatchViewResponse =
		sessions.current(currentAccountId(), credential).let {
			matches.current(it.session.accountId, it.session.trainerId)
		}

	@GetMapping("/{matchId}")
	fun find(
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable matchId: String,
	): MatchViewResponse = sessions.current(currentAccountId(), credential).let {
		matches.view(it.session.accountId, it.session.trainerId, matchId.pathIdentifier("matchId"))
	}

	@PostMapping("/{matchId}/turns")
	fun submitTurn(
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable matchId: String,
		@RequestBody request: SubmitMatchTurnRequest,
	): MatchTurnResponse = sessions.current(currentAccountId(), credential).let {
		matches.submitTurn(it.session.accountId, it.session.trainerId, matchId.pathIdentifier("matchId"), request).also { result ->
			result.match?.let { changed -> events.matchChanged(changed.id, changed.revision) }
		}
	}

	@PostMapping("/{matchId}/forfeit")
	fun forfeit(
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable matchId: String,
		@RequestBody request: ForfeitMatchRequest,
	): MatchViewResponse = sessions.current(currentAccountId(), credential).let {
		matches.forfeit(it.session.accountId, it.session.trainerId, matchId.pathIdentifier("matchId"), request.expectedRevision).also { changed ->
			events.matchChanged(changed.id, changed.revision)
		}
	}
}
