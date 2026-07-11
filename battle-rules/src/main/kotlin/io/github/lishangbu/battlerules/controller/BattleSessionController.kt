package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.battlerules.dto.BattleSessionCreateRequest
import io.github.lishangbu.battlerules.dto.BattleSessionResponse
import io.github.lishangbu.battlerules.dto.BattleSessionSummaryPageResponse
import io.github.lishangbu.battlerules.dto.BattleSessionSummaryResponse
import io.github.lishangbu.battlerules.dto.BattleSessionTurnCommandRequest
import io.github.lishangbu.battlerules.dto.BattleSessionTurnRecordPageResponse
import io.github.lishangbu.battlerules.dto.BattleSessionTurnRecordResponse
import io.github.lishangbu.battlerules.dto.BattleSessionTurnResponse
import io.github.lishangbu.battlerules.dto.BattleSessionTerminationRequest
import io.github.lishangbu.battlesession.BattleSessionStatus
import io.github.lishangbu.battlerules.openapi.BATTLE_SESSIONS_API_BEARER_AUTH
import io.github.lishangbu.battlerules.openapi.BATTLE_SESSIONS_API_BAD_REQUEST_DESCRIPTION
import io.github.lishangbu.battlerules.openapi.BATTLE_SESSIONS_API_CONFLICT_DESCRIPTION
import io.github.lishangbu.battlerules.openapi.BATTLE_SESSIONS_API_FORBIDDEN_DESCRIPTION
import io.github.lishangbu.battlerules.openapi.BATTLE_SESSIONS_API_NOT_FOUND_DESCRIPTION
import io.github.lishangbu.battlerules.openapi.BATTLE_SESSIONS_API_SCOPE
import io.github.lishangbu.battlerules.openapi.BATTLE_SESSIONS_API_UNAUTHORIZED_DESCRIPTION
import io.github.lishangbu.common.web.ApiErrorResponse
import io.github.lishangbu.battlerules.service.BattleSessionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.babyfish.jimmer.Page
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** 进程内临时 Battle Session 管理 API。 */
@RestController
@RequestMapping("/api/battle-sessions")
@Tag(name = "战斗会话")
@SecurityRequirement(name = BATTLE_SESSIONS_API_BEARER_AUTH, scopes = [BATTLE_SESSIONS_API_SCOPE])
class BattleSessionController(
	private val service: BattleSessionService,
) {
	@GetMapping
	@Operation(operationId = "listBattleSessions", summary = "分页查询战斗会话")
	@ApiResponses(
		ApiResponse(
			responseCode = "200",
			description = "战斗会话分页列表。",
			content = [Content(mediaType = "application/json", schema = Schema(implementation = BattleSessionSummaryPageResponse::class))],
		),
		ApiResponse(responseCode = "400", description = BATTLE_SESSIONS_API_BAD_REQUEST_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		ApiResponse(responseCode = "401", description = BATTLE_SESSIONS_API_UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		ApiResponse(responseCode = "403", description = BATTLE_SESSIONS_API_FORBIDDEN_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
	)
	fun list(
		@RequestParam(required = false) status: BattleSessionStatus?,
		@RequestParam(required = false) formatCode: String?,
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "20") size: Int,
	): Page<BattleSessionSummaryResponse> =
		service.list(status, formatCode, page, size)

	@GetMapping("/{sessionId}")
	@Operation(operationId = "getBattleSession", summary = "读取战斗会话")
	@ApiResponses(
		ApiResponse(
			responseCode = "200",
			description = "战斗会话详情。",
			content = [Content(mediaType = "application/json", schema = Schema(implementation = BattleSessionResponse::class))],
		),
		ApiResponse(
			responseCode = "404",
			description = BATTLE_SESSIONS_API_NOT_FOUND_DESCRIPTION,
			content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))],
		),
		ApiResponse(responseCode = "401", description = BATTLE_SESSIONS_API_UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		ApiResponse(responseCode = "403", description = BATTLE_SESSIONS_API_FORBIDDEN_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
	)
	fun get(@PathVariable sessionId: String): BattleSessionResponse =
		service.get(sessionId)

	@GetMapping("/{sessionId}/turns")
	@Operation(operationId = "listBattleSessionTurns", summary = "分页查询战斗会话回合")
	@ApiResponses(
		ApiResponse(
			responseCode = "200",
			description = "Turn Record 分页列表。",
			content = [Content(mediaType = "application/json", schema = Schema(implementation = BattleSessionTurnRecordPageResponse::class))],
		),
		ApiResponse(responseCode = "400", description = BATTLE_SESSIONS_API_BAD_REQUEST_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		ApiResponse(responseCode = "401", description = BATTLE_SESSIONS_API_UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		ApiResponse(responseCode = "403", description = BATTLE_SESSIONS_API_FORBIDDEN_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		ApiResponse(responseCode = "404", description = BATTLE_SESSIONS_API_NOT_FOUND_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
	)
	fun listTurns(
		@PathVariable sessionId: String,
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "20") size: Int,
	): Page<BattleSessionTurnRecordResponse> =
		service.listTurns(sessionId, page, size)

	@PostMapping("/{sessionId}/turns")
	@Operation(operationId = "submitBattleSessionTurn", summary = "结算战斗会话回合")
	@ApiResponses(
		ApiResponse(
			responseCode = "200",
			description = "回合结算结果。",
			content = [Content(mediaType = "application/json", schema = Schema(implementation = BattleSessionTurnResponse::class))],
		),
		ApiResponse(
			responseCode = "409",
			description = BATTLE_SESSIONS_API_CONFLICT_DESCRIPTION,
			content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))],
		),
		ApiResponse(responseCode = "400", description = BATTLE_SESSIONS_API_BAD_REQUEST_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		ApiResponse(responseCode = "401", description = BATTLE_SESSIONS_API_UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		ApiResponse(responseCode = "403", description = BATTLE_SESSIONS_API_FORBIDDEN_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		ApiResponse(responseCode = "404", description = BATTLE_SESSIONS_API_NOT_FOUND_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
	)
	fun submitTurn(
		@PathVariable sessionId: String,
		@RequestBody request: BattleSessionTurnCommandRequest,
	): BattleSessionTurnResponse =
		service.submitTurn(sessionId, request)

	@PostMapping("/{sessionId}/termination")
	@Operation(operationId = "terminateBattleSession", summary = "终止战斗会话")
	@ApiResponses(
		ApiResponse(responseCode = "200", description = "终止后的战斗会话。", content = [Content(mediaType = "application/json", schema = Schema(implementation = BattleSessionResponse::class))]),
		ApiResponse(responseCode = "400", description = BATTLE_SESSIONS_API_BAD_REQUEST_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		ApiResponse(responseCode = "401", description = BATTLE_SESSIONS_API_UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		ApiResponse(responseCode = "403", description = BATTLE_SESSIONS_API_FORBIDDEN_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		ApiResponse(responseCode = "404", description = BATTLE_SESSIONS_API_NOT_FOUND_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		ApiResponse(responseCode = "409", description = BATTLE_SESSIONS_API_CONFLICT_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
	)
	fun terminate(
		@PathVariable sessionId: String,
		@RequestBody request: BattleSessionTerminationRequest,
	): BattleSessionResponse =
		service.terminate(sessionId, request)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(operationId = "createBattleSession", summary = "创建战斗会话")
	@ApiResponses(
		ApiResponse(
			responseCode = "201",
			description = "已创建战斗会话。",
			content = [Content(mediaType = "application/json", schema = Schema(implementation = BattleSessionResponse::class))],
		),
		ApiResponse(
			responseCode = "503",
			description = "Session Runtime 活跃容量已满。",
			headers = [
				Header(
					name = "Retry-After",
					description = "建议等待的秒数。",
					schema = Schema(type = "string"),
				),
			],
			content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))],
		),
		ApiResponse(responseCode = "400", description = BATTLE_SESSIONS_API_BAD_REQUEST_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		ApiResponse(responseCode = "401", description = BATTLE_SESSIONS_API_UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		ApiResponse(responseCode = "403", description = BATTLE_SESSIONS_API_FORBIDDEN_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
	)
	fun create(@RequestBody request: BattleSessionCreateRequest): BattleSessionResponse =
		service.create(request)
}
