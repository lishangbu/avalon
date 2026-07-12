package io.github.lishangbu.match.trainer

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

/**
 * Trainer Session 的进入、当前状态和退出入口。
 *
 * 进入只信任 OAuth 账户与其拥有的 Trainer；后续操作同时校验 Bearer 和 `X-Trainer-Session`。
 */
@RestController
@RequestMapping("/api/player/trainer-session")
class TrainerSessionController(private val service: TrainerSessionService) {
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	fun enter(authentication: Authentication, @RequestBody request: EnterTrainerSessionRequest): TrainerSessionResponse =
		mapErrors { TrainerSessionResponse.from(service.enter(authentication.accountId(), request.trainerId.toLong())) }

	@GetMapping
	fun current(authentication: Authentication, @RequestHeader("X-Trainer-Session") credential: String): TrainerSessionResponse =
		mapErrors { TrainerSessionResponse.from(service.current(authentication.accountId(), credential)) }

	@PostMapping("/heartbeat")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun heartbeat(authentication: Authentication, @RequestHeader("X-Trainer-Session") credential: String) =
		mapErrors { service.heartbeat(authentication.accountId(), credential) }

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun leave(authentication: Authentication, @RequestHeader("X-Trainer-Session") credential: String) =
		service.leave(authentication.accountId(), credential)

	private fun <T> mapErrors(action: () -> T): T = try { action() } catch (error: RuntimeException) {
		when (error) {
			is TrainerUnavailableException, is NumberFormatException -> throw TrainerSessionRequestException(HttpStatus.NOT_FOUND, "trainer.unavailable")
			is TrainerSwitchBlockedException, is TrainerSessionEntryBlockedException -> throw TrainerSessionRequestException(HttpStatus.CONFLICT, "trainer-session.switch-blocked")
			is InvalidTrainerSessionException -> throw TrainerSessionRequestException(HttpStatus.UNAUTHORIZED, "trainer-session.invalid")
			else -> throw error
		}
	}
}
