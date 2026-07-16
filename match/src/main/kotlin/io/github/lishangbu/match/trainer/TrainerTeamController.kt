package io.github.lishangbu.match.trainer

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** 同时验证登录账户与 Trainer Session 的多 Team 读写入口。 */
@RestController
@RequestMapping("/api/player/trainer-teams")
class TrainerTeamController(
	private val sessions: TrainerSessionService,
	private val teams: TrainerTeamService,
	private val shares: TrainerTeamShareService,
) {
	@GetMapping
	fun list(@RequestHeader("X-Trainer-Session") credential: String): List<TrainerTeamResponse> =
		teams.list(currentTrainerId(credential)).map(TrainerTeamRecord::toResponse)

	@GetMapping("/{teamId}")
	fun get(
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable teamId: Long,
	): TrainerTeamResponse = teams.get(currentTrainerId(credential), teamId).toResponse()

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	fun create(
		@RequestHeader("X-Trainer-Session") credential: String,
		@RequestBody request: SaveTrainerTeamRequest,
	): TrainerTeamResponse = teams.create(currentTrainerId(credential), request).toResponse()

	@PutMapping("/{teamId}")
	fun update(
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable teamId: Long,
		@RequestBody request: SaveTrainerTeamRequest,
	): TrainerTeamResponse = teams.update(currentTrainerId(credential), teamId, request).toResponse()

	@PostMapping("/{teamId}/activate")
	fun activate(
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable teamId: Long,
	): TrainerTeamResponse = teams.activate(currentTrainerId(credential), teamId).toResponse()

	@PostMapping("/{teamId}/shares")
	@ResponseStatus(HttpStatus.CREATED)
	fun share(
		@RequestHeader("X-Trainer-Session") credential: String,
		@PathVariable teamId: Long,
	): TrainerTeamShareResponse = shares.share(currentTrainerId(credential), teamId)

	@PostMapping("/imports")
	@ResponseStatus(HttpStatus.CREATED)
	fun import(
		@RequestHeader("X-Trainer-Session") credential: String,
		@RequestBody request: ImportTrainerTeamRequest,
	): TrainerTeamResponse = shares.import(currentTrainerId(credential), request).toResponse()

	private fun currentTrainerId(credential: String): Long = try {
		sessions.current(currentAccountId(), credential).trainer.id
	} catch (error: InvalidTrainerSessionException) {
		throw TrainerSessionRequestException(HttpStatus.UNAUTHORIZED, "trainer-session.invalid")
	}
}
