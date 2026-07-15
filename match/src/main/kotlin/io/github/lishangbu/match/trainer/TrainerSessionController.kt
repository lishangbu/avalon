package io.github.lishangbu.match.trainer

import cn.dev33.satoken.stp.StpUtil
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * Trainer Session 的进入、当前状态和退出入口。
 *
 * 进入只信任 Sa-Token 登录账户与其拥有的 Trainer；后续操作同时校验登录和 `X-Trainer-Session`。
 */
@RestController
@RequestMapping("/api/player/trainer-session")
class TrainerSessionController(private val service: TrainerSessionService) {
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	fun enter(@RequestBody request: EnterTrainerSessionRequest): TrainerSessionResponse =
		mapErrors {
			TrainerSessionResponse.from(
				service.enter(currentAccountId(), request.trainerId.toLong(), StpUtil.getTokenValue()),
			)
		}

	@GetMapping
	fun current(@RequestHeader("X-Trainer-Session") credential: String): TrainerSessionResponse =
		mapErrors { TrainerSessionResponse.from(service.current(currentAccountId(), credential)) }

	@PostMapping("/heartbeat")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun heartbeat(@RequestHeader("X-Trainer-Session") credential: String) =
		mapErrors { service.heartbeat(currentAccountId(), credential) }

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun leave(@RequestHeader("X-Trainer-Session") credential: String) =
		service.leave(currentAccountId(), credential)

	private fun <T> mapErrors(action: () -> T): T = try { action() } catch (error: RuntimeException) {
		when (error) {
			is TrainerUnavailableException, is NumberFormatException -> throw TrainerSessionRequestException(HttpStatus.NOT_FOUND, "trainer.unavailable")
			is TrainerSwitchBlockedException, is TrainerSessionEntryBlockedException -> throw TrainerSessionRequestException(HttpStatus.CONFLICT, "trainer-session.switch-blocked")
			is InvalidTrainerSessionException -> throw TrainerSessionRequestException(HttpStatus.UNAUTHORIZED, "trainer-session.invalid")
			else -> throw error
		}
	}
}
