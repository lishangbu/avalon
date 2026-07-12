package io.github.lishangbu.match.trainer

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/player/trainers")
class TrainerController(private val service: TrainerService) {
	@GetMapping
	fun list(authentication: Authentication): List<TrainerResponse> =
		service.list(authentication.accountId()).map(TrainerResponse::from)

	@GetMapping("/archived")
	fun listArchived(authentication: Authentication): List<TrainerResponse> =
		service.listArchived(authentication.accountId()).map(TrainerResponse::from)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	fun create(authentication: Authentication, @RequestBody request: CreateTrainerRequest): TrainerResponse =
		mapErrors { TrainerResponse.from(service.create(authentication.accountId(), CreateTrainerCommand(UUID.fromString(request.commandId), request.displayName))) }

	@PostMapping("/{trainerId}/archive")
	fun archive(authentication: Authentication, @PathVariable trainerId: Long, @RequestBody request: RevisionRequest): TrainerResponse =
		mapErrors { TrainerResponse.from(service.archive(authentication.accountId(), trainerId, request.requiredRevision())) }

	@PostMapping("/{trainerId}/restore")
	fun restore(authentication: Authentication, @PathVariable trainerId: Long, @RequestBody request: RevisionRequest): TrainerResponse =
		mapErrors { TrainerResponse.from(service.restore(authentication.accountId(), trainerId, request.requiredRevision())) }

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

@RestController
@RequestMapping("/api/player/trainer-session")
class TrainerSessionController(private val service: TrainerSessionService) {
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	fun enter(authentication: Authentication, @RequestBody request: EnterTrainerSessionRequest): TrainerSessionResponse =
		mapSessionErrors { TrainerSessionResponse.from(service.enter(authentication.accountId(), request.trainerId.toLong())) }

	@GetMapping
	fun current(authentication: Authentication, @RequestHeader("X-Trainer-Session") credential: String): TrainerSessionResponse =
		mapSessionErrors { TrainerSessionResponse.from(service.current(authentication.accountId(), credential)) }

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun leave(authentication: Authentication, @RequestHeader("X-Trainer-Session") credential: String) =
		service.leave(authentication.accountId(), credential)

	private fun <T> mapSessionErrors(action: () -> T): T = try { action() } catch (error: RuntimeException) {
		when (error) {
			is TrainerUnavailableException, is NumberFormatException -> throw TrainerSessionRequestException(HttpStatus.NOT_FOUND, "trainer.unavailable")
			is TrainerSwitchBlockedException, is TrainerSessionEntryBlockedException -> throw TrainerSessionRequestException(HttpStatus.CONFLICT, "trainer-session.switch-blocked")
			is InvalidTrainerSessionException -> throw TrainerSessionRequestException(HttpStatus.UNAUTHORIZED, "trainer-session.invalid")
			else -> throw error
		}
	}
}

class TrainerSessionRequestException(val status: HttpStatus, val code: String) : RuntimeException(code)
data class TrainerSessionErrorResponse(val code: String, val message: String)

@RestControllerAdvice(assignableTypes = [TrainerSessionController::class])
class TrainerSessionExceptionHandler {
	@ExceptionHandler(TrainerSessionRequestException::class)
	fun handle(error: TrainerSessionRequestException): ResponseEntity<TrainerSessionErrorResponse> =
		ResponseEntity.status(error.status).body(TrainerSessionErrorResponse(error.code, error.code))
}

data class CreateTrainerRequest(var commandId: String = "", var displayName: String = "")
data class RevisionRequest(var expectedRevision: Long? = null)
data class TrainerResponse(val id: String, val displayName: String, val revision: Long, val archivedAt: Instant?) {
	companion object { fun from(record: TrainerRecord) = TrainerResponse(record.id.toString(), record.displayName, record.revision, record.archivedAt) }
}
data class EnterTrainerSessionRequest(var trainerId: String = "")
data class TrainerSessionResponse(val credential: String, val expiresAt: Instant, val trainer: TrainerResponse) {
	companion object {
		fun from(view: TrainerSessionView) = TrainerSessionResponse(
			view.session.credential,
			view.session.expiresAt,
			TrainerResponse.from(view.trainer),
		)
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
