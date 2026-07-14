package io.github.lishangbu.match.trainer

import cn.dev33.satoken.stp.StpUtil
import io.github.lishangbu.match.game.MatchHistoryResponse
import io.github.lishangbu.match.game.MatchService
import io.github.lishangbu.match.game.MatchViewResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.dao.DataIntegrityViolationException
import java.util.UUID

/** 只依赖当前登录账户身份的 Trainer 生命周期 REST 入口。 */
@RestController
@RequestMapping("/api/player/trainers")
class TrainerController(private val service: TrainerService, private val matches: MatchService) {
	@GetMapping
	fun list(): List<TrainerResponse> =
		service.list(currentAccountId()).map(TrainerRecord::toResponse)

	@GetMapping("/archived")
	fun listArchived(): List<TrainerResponse> =
		service.listArchived(currentAccountId()).map(TrainerRecord::toResponse)

	/** 归档 Trainer 无法建立 Session，其历史由所属登录账户只读访问。 */
	@GetMapping("/{trainerId}/match-history")
	fun matchHistory(
		@PathVariable trainerId: Long,
		@RequestParam(required = false) beforeMatchId: String?,
		@RequestParam(defaultValue = "20") limit: Int,
	): List<MatchHistoryResponse> = matches.history(
		currentAccountId(), trainerId, archivedOnly = true,
		beforeMatchId = beforeMatchId?.toLongOrNull() ?: Long.MAX_VALUE, limit = limit,
	)

	@GetMapping("/{trainerId}/match-history/{matchId}")
	fun matchHistoryDetail(
		@PathVariable trainerId: Long,
		@PathVariable matchId: String,
	): MatchViewResponse = matches.historyDetail(
		currentAccountId(), trainerId, matchId.toLongOrNull() ?: -1, archivedOnly = true,
	)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	fun create(@RequestBody request: CreateTrainerRequest): TrainerResponse =
		mapErrors { service.create(currentAccountId(), CreateTrainerCommand(UUID.fromString(request.commandId), request.displayName)).toResponse() }

	@PostMapping("/{trainerId}/archive")
	fun archive(@PathVariable trainerId: Long, @RequestBody request: RevisionRequest): TrainerResponse =
		mapErrors { service.archive(currentAccountId(), trainerId, request.requiredRevision()).toResponse() }

	@PostMapping("/{trainerId}/restore")
	fun restore(@PathVariable trainerId: Long, @RequestBody request: RevisionRequest): TrainerResponse =
		mapErrors { service.restore(currentAccountId(), trainerId, request.requiredRevision()).toResponse() }

	private fun <T> mapErrors(action: () -> T): T = try { action() } catch (error: RuntimeException) {
		when (error) {
			is InvalidTrainerDisplayNameException, is SensitiveTrainerDisplayNameException, is IllegalArgumentException -> throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "trainer.request.invalid")
			is TrainerLimitExceededException -> throw ResponseStatusException(HttpStatus.CONFLICT, "trainer.limit-exceeded")
			is TrainerArchiveBlockedException -> throw ResponseStatusException(HttpStatus.CONFLICT, "trainer.archive-blocked")
			is TrainerCommandPayloadConflictException, is TrainerRevisionConflictException -> throw ResponseStatusException(HttpStatus.CONFLICT, "trainer.conflict")
			is DataIntegrityViolationException -> throw ResponseStatusException(HttpStatus.CONFLICT, "trainer.conflict")
			else -> throw error
		}
	}
}
private fun RevisionRequest.requiredRevision(): Long =
	expectedRevision ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "expectedRevision.required")

internal fun currentAccountId(): Long = StpUtil.getLoginIdAsLong()
