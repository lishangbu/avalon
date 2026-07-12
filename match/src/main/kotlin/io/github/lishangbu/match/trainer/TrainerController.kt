package io.github.lishangbu.match.trainer

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.dao.DataIntegrityViolationException
import java.util.UUID

/** 只依赖 OAuth 账户身份的 Trainer 生命周期 REST 入口。 */
@RestController
@RequestMapping("/api/player/trainers")
class TrainerController(private val service: TrainerService) {
	@GetMapping
	fun list(authentication: Authentication): List<TrainerResponse> =
		service.list(authentication.accountId()).map(TrainerRecord::toResponse)

	@GetMapping("/archived")
	fun listArchived(authentication: Authentication): List<TrainerResponse> =
		service.listArchived(authentication.accountId()).map(TrainerRecord::toResponse)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	fun create(authentication: Authentication, @RequestBody request: CreateTrainerRequest): TrainerResponse =
		mapErrors { service.create(authentication.accountId(), CreateTrainerCommand(UUID.fromString(request.commandId), request.displayName)).toResponse() }

	@PostMapping("/{trainerId}/archive")
	fun archive(authentication: Authentication, @PathVariable trainerId: Long, @RequestBody request: RevisionRequest): TrainerResponse =
		mapErrors { service.archive(authentication.accountId(), trainerId, request.requiredRevision()).toResponse() }

	@PostMapping("/{trainerId}/restore")
	fun restore(authentication: Authentication, @PathVariable trainerId: Long, @RequestBody request: RevisionRequest): TrainerResponse =
		mapErrors { service.restore(authentication.accountId(), trainerId, request.requiredRevision()).toResponse() }

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

internal fun Authentication.accountId(): Long {
	val value = when (val value = principal) {
		is OAuth2AuthenticatedPrincipal -> value.getAttribute<String>("account_id")
		is Jwt -> value.getClaimAsString("account_id")
		else -> null
	}
	return value?.toLongOrNull() ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
}
