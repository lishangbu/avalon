package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattlePreparationParticipantRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationSideRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationValidationRequest
import io.github.lishangbu.battlerules.dto.BattleSessionCreateRequest
import io.github.lishangbu.battlerules.dto.BattleSessionResponse
import io.github.lishangbu.battlerules.dto.BattleSessionSummaryResponse
import io.github.lishangbu.battlerules.dto.BattleActionRequest
import io.github.lishangbu.battlerules.dto.BattleSessionTurnCommandRequest
import io.github.lishangbu.battlerules.dto.BattleSessionTurnRecordResponse
import io.github.lishangbu.battlerules.dto.BattleSessionTurnResponse
import io.github.lishangbu.battlerules.dto.BattleSessionTerminationRequest
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battlesession.BattleSessionRuntime
import io.github.lishangbu.battlesession.model.BattleSessionStatus
import io.github.lishangbu.battlesession.model.SessionQuery
import io.github.lishangbu.battlesession.model.TerminationCommand
import io.github.lishangbu.battlesession.model.TurnCommand
import io.github.lishangbu.battlesession.roster.SessionRosterIdentifiers
import io.github.lishangbu.battlesession.roster.SessionRosterSideInput
import io.github.lishangbu.common.web.ApiErrorCode
import io.github.lishangbu.common.web.ApiException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.babyfish.jimmer.Page
import io.github.lishangbu.common.web.filterValue
import io.github.lishangbu.common.web.validatePage
import java.util.Locale
import java.util.UUID

/** Battle Session 的资料装配与 Runtime 应用服务。 */
@Service
class BattleSessionService(
	private val snapshotService: BattleRuntimeSnapshotService,
	private val runtime: BattleSessionRuntime,
	private val responseMapper: BattleSessionResponseMapper,
) {
	private val rosterIdentifiers = SessionRosterIdentifiers()

	fun create(request: BattleSessionCreateRequest): BattleSessionResponse {
		if (request.formatCode.isBlank()) {
			invalidRequest("formatCode", "formatCode 不能为空")
		}
		val activeParticipantsPerSide = request.sides.firstOrNull()?.activeParticipantIndexes?.size
			?: invalidRequest("sides", "sides 必须包含双方阵容")
		val layout = try {
			rosterIdentifiers.assign(
				sides = request.sides.map { side ->
					SessionRosterSideInput(
						participantCount = side.participants.size,
						activeParticipantIndexes = side.activeParticipantIndexes,
					)
				},
				activeParticipantsPerSide = activeParticipantsPerSide,
			)
		} catch (exception: IllegalArgumentException) {
			invalidRequest("sides", exception.message ?: "sides 阵容配置不合法")
		}
		val preparationRequest = BattlePreparationValidationRequest(
			formatCode = request.formatCode.trim(),
			sides = request.sides.zip(layout.sides).map { (side, identifiers) ->
				BattlePreparationSideRequest(
					sideId = identifiers.sideId,
					activeActorIds = identifiers.activeActorIds,
					participants = side.participants.zip(identifiers.actorIds).map { (participant, actorId) ->
						BattlePreparationParticipantRequest(
							actorId = actorId,
							creatureId = participant.creatureId,
							level = participant.level,
							skillIds = participant.skillIds,
							gender = participant.gender,
							abilityId = participant.abilityId,
							itemId = participant.itemId,
							individualValues = participant.individualValues,
							effortValues = participant.effortValues,
							natureIncreasedStat = participant.natureIncreasedStat,
							natureDecreasedStat = participant.natureDecreasedStat,
							teraElementId = participant.teraElementId,
						)
					},
				)
			},
		)
		val initialState = snapshotService.assembleBattleSessionInitialState(preparationRequest)
		return responseMapper.toResponse(runtime.create(initialState))
	}

	fun get(sessionId: String): BattleSessionResponse =
		responseMapper.toResponse(runtime.get(sessionId))

	fun list(
		status: BattleSessionStatus?,
		formatCode: String?,
		page: Int,
		size: Int,
	): Page<BattleSessionSummaryResponse> {
		validatePage(page, size)
		val sessions = runtime.list(
			SessionQuery(
				status = status,
				formatCode = filterValue("formatCode", formatCode),
				page = page,
				size = size,
			),
		)
		return Page(
			sessions.items.map(responseMapper::toSummary),
			sessions.totalElements,
			sessions.totalPages.toLong(),
		)
	}

	fun submitTurn(sessionId: String, request: BattleSessionTurnCommandRequest): BattleSessionTurnResponse {
		val command = TurnCommand(
			commandId = validCommandId(request.commandId),
			expectedRevision = validExpectedRevision(request.expectedRevision),
			actions = request.actions.map(::toBattleAction),
		)
		val result = runtime.submitTurn(sessionId, command)
		return BattleSessionTurnResponse(
			session = responseMapper.toResponse(result.session),
			turnRecord = responseMapper.toTurnRecord(result.turnRecord),
		)
	}

	fun terminate(sessionId: String, request: BattleSessionTerminationRequest): BattleSessionResponse {
		val result = runtime.terminate(
			sessionId = sessionId,
			command = TerminationCommand(
				commandId = validCommandId(request.commandId),
				expectedRevision = validExpectedRevision(request.expectedRevision),
				reason = requiredText(request.reason, "reason"),
			),
		)
		return responseMapper.toResponse(result.session)
	}

	fun listTurns(sessionId: String, page: Int, size: Int): Page<BattleSessionTurnRecordResponse> {
		validatePage(page, size)
		val records = runtime.get(sessionId).turnRecords.asReversed()
		val fromIndex = (page.toLong() * size).coerceAtMost(records.size.toLong()).toInt()
		val toIndex = (fromIndex + size).coerceAtMost(records.size)
		val totalPages = if (records.isEmpty()) 0L else (records.size.toLong() - 1) / size + 1
		return Page(
			records.subList(fromIndex, toIndex).map(responseMapper::toTurnRecord),
			records.size.toLong(),
			totalPages,
		)
	}

	private fun toBattleAction(request: BattleActionRequest): BattleAction =
		when (request.type.trim().uppercase(Locale.ROOT)) {
			"USE_SKILL" -> BattleAction.UseSkill(
				actorId = requiredText(request.actorId, "actions"),
				skillId = request.skillId?.takeIf { it > 0 }
					?: invalidRequest("actions", "使用技能时 skillId 必须为正数"),
				targetActorId = requiredText(request.targetActorId, "actions"),
			)

			"SWITCH_PARTICIPANT" -> {
				if (request.skillId != null) {
					invalidRequest("actions", "替换成员时不能携带 skillId")
				}
				BattleAction.SwitchParticipant(
					actorId = requiredText(request.actorId, "actions"),
					targetActorId = requiredText(request.targetActorId, "actions"),
				)
			}

			else -> invalidRequest("actions", "actions.type 不是合法值")
		}

	private fun validCommandId(value: String): String {
		val commandId = requiredText(value, "commandId")
		val uuid = runCatching { UUID.fromString(commandId) }
			.getOrElse { invalidRequest("commandId", "commandId 必须是 UUID v4") }
		if (uuid.version() != 4) {
			invalidRequest("commandId", "commandId 必须是 UUID v4")
		}
		return commandId
	}

	private fun validExpectedRevision(value: Long): Long {
		if (value < 0) {
			invalidRequest("expectedRevision", "expectedRevision 不能小于 0")
		}
		return value
	}

	private fun requiredText(value: String, field: String): String =
		value.trim().takeIf { it.isNotEmpty() }
			?: invalidRequest(field, "$field 不能为空")

	private fun invalidRequest(field: String, message: String): Nothing =
		throw ApiException(
			status = HttpStatus.BAD_REQUEST,
			code = ApiErrorCode.VALIDATION_INVALID,
			message = message,
			field = field,
		)
}
