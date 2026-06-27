package io.github.lishangbu.gamedata.controller

import io.github.lishangbu.gamedata.dto.*
import io.github.lishangbu.gamedata.support.toGameDataFilters
import io.github.lishangbu.common.web.ApiErrorResponse
import io.github.lishangbu.gamedata.service.GameLocationAreasService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 地点区域管理接口。
 */
@RestController
@RequestMapping("/api/game-data/location-areas")
@Tag(name = "游戏资料 - 地点区域")
@SecurityRequirement(name = GAME_DATA_API_BEARER_AUTH)
class GameLocationAreasController(
	private val service: GameLocationAreasService,
) {
	@Operation(summary = "分页查询地点区域")
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "查询成功", content = [Content(mediaType = "application/json", schema = Schema(implementation = GameDataPageResponse::class))]),
			ApiResponse(responseCode = "400", description = GAME_DATA_API_BAD_REQUEST_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = GAME_DATA_API_UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = GAME_DATA_API_FORBIDDEN_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	@GetMapping
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) q: String?,
		request: HttpServletRequest,
	): GameDataPageResponse<GameLocationAreasResponse> = service.list(page, size, q, request.toGameDataFilters())

	@Operation(summary = "读取单条地点区域")
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "读取成功", content = [Content(mediaType = "application/json", schema = Schema(implementation = GameLocationAreasResponse::class))]),
			ApiResponse(responseCode = "401", description = GAME_DATA_API_UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = GAME_DATA_API_FORBIDDEN_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = GAME_DATA_API_NOT_FOUND_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	@GetMapping("/{id}")
	fun get(@PathVariable id: Long): GameLocationAreasResponse = service.get(id)

	@Operation(summary = "新增地点区域")
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "新增成功", content = [Content(mediaType = "application/json", schema = Schema(implementation = GameLocationAreasResponse::class))]),
			ApiResponse(responseCode = "400", description = GAME_DATA_API_BAD_REQUEST_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = GAME_DATA_API_UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = GAME_DATA_API_FORBIDDEN_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "409", description = GAME_DATA_API_CONFLICT_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	@PostMapping
	fun create(@RequestBody request: GameLocationAreasRequest): GameLocationAreasResponse = service.create(request)

	@Operation(summary = "修改地点区域")
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "修改成功", content = [Content(mediaType = "application/json", schema = Schema(implementation = GameLocationAreasResponse::class))]),
			ApiResponse(responseCode = "400", description = GAME_DATA_API_BAD_REQUEST_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = GAME_DATA_API_UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = GAME_DATA_API_FORBIDDEN_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = GAME_DATA_API_NOT_FOUND_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "409", description = GAME_DATA_API_CONFLICT_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	@PutMapping("/{id}")
	fun update(@PathVariable id: Long, @RequestBody request: GameLocationAreasRequest): GameLocationAreasResponse = service.update(id, request)

	@Operation(summary = "删除地点区域")
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "204", description = "删除成功"),
			ApiResponse(responseCode = "401", description = GAME_DATA_API_UNAUTHORIZED_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = GAME_DATA_API_FORBIDDEN_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = GAME_DATA_API_NOT_FOUND_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "409", description = GAME_DATA_API_CONFLICT_DESCRIPTION, content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
