package io.github.lishangbu.gamedata.api

import io.github.lishangbu.common.web.ApiErrorResponse
import io.github.lishangbu.gamedata.table.GameDataTableService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * 游戏资料 Controller 的通用 CRUD 协议。
 */
@SecurityRequirement(name = GAME_DATA_API_BEARER_AUTH)
abstract class GameDataCrudControllerSupport(
	private val service: GameDataTableService,
) {
	@Operation(summary = "分页查询游戏资料")
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "查询成功"),
			ApiResponse(responseCode = "400", description = GAME_DATA_API_BAD_REQUEST_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = GAME_DATA_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = GAME_DATA_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	@GetMapping
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) q: String?,
	): GameDataPageResponse = service.list(page, size, q)

	@Operation(summary = "读取单条游戏资料")
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "读取成功"),
			ApiResponse(responseCode = "401", description = GAME_DATA_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = GAME_DATA_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = GAME_DATA_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	@GetMapping("/{id}")
	fun get(@PathVariable id: Long): GameDataRecordResponse = service.get(id)

	@Operation(summary = "新增游戏资料")
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "新增成功"),
			ApiResponse(responseCode = "400", description = GAME_DATA_API_BAD_REQUEST_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = GAME_DATA_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = GAME_DATA_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "409", description = GAME_DATA_API_CONFLICT_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	@PostMapping
	fun create(@RequestBody request: GameDataRecordRequest): GameDataRecordResponse = service.create(request)

	@Operation(summary = "修改游戏资料")
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "修改成功"),
			ApiResponse(responseCode = "400", description = GAME_DATA_API_BAD_REQUEST_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = GAME_DATA_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = GAME_DATA_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = GAME_DATA_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "409", description = GAME_DATA_API_CONFLICT_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	@PutMapping("/{id}")
	fun update(@PathVariable id: Long, @RequestBody request: GameDataRecordRequest): GameDataRecordResponse = service.update(id, request)

	@Operation(summary = "删除游戏资料")
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "204", description = "删除成功"),
			ApiResponse(responseCode = "401", description = GAME_DATA_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = GAME_DATA_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = GAME_DATA_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "409", description = GAME_DATA_API_CONFLICT_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
