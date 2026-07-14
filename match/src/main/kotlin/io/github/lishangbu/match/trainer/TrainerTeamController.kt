package io.github.lishangbu.match.trainer

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 同时验证登录账户与 Trainer Session 的唯一 Team 读写入口。 */
@RestController
@RequestMapping("/api/player/trainer-team")
class TrainerTeamController(
	private val sessions: TrainerSessionService,
	private val teams: TrainerTeamService,
) {
	@GetMapping
	fun get(@RequestHeader("X-Trainer-Session") credential: String): TrainerTeamResponse {
		val trainerId = currentTrainerId(credential)
		return (teams.find(trainerId) ?: throw TrainerTeamRequestException("trainer-team.not-found")).toResponse()
	}

	@PutMapping
	fun save(
		@RequestHeader("X-Trainer-Session") credential: String,
		@RequestBody request: SaveTrainerTeamRequest,
	): TrainerTeamResponse = teams.save(currentTrainerId(credential), request).toResponse()

	private fun currentTrainerId(credential: String): Long = try {
		sessions.current(currentAccountId(), credential).trainer.id
	} catch (error: InvalidTrainerSessionException) {
		throw TrainerSessionRequestException(HttpStatus.UNAUTHORIZED, "trainer-session.invalid")
	}
}
