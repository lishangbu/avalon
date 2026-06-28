package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.battlerules.dto.BattleFormatSpecialMechanicRequest
import io.github.lishangbu.battlerules.dto.BattleFormatSpecialMechanicResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleFormatSpecialMechanicService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.babyfish.jimmer.Page
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
 * 战斗赛制特殊机制绑定管理 API。
 */
@RestController
@RequestMapping("/api/battle-rules/format-special-mechanics")
@Tag(name = "战斗规则 - 赛制特殊机制")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleFormatSpecialMechanicController(
	private val service: BattleFormatSpecialMechanicService,
) {
	@GetMapping
	@Operation(summary = "分页查询赛制特殊机制绑定")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) formatId: Long?,
		@RequestParam(required = false) mechanicId: Long?,
	): Page<BattleFormatSpecialMechanicResponse> =
		service.list(page, size, formatId, mechanicId)

	@GetMapping("/{id}")
	@Operation(summary = "读取赛制特殊机制绑定")
	fun get(@PathVariable id: Long): BattleFormatSpecialMechanicResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增赛制特殊机制绑定")
	fun create(@RequestBody request: BattleFormatSpecialMechanicRequest): BattleFormatSpecialMechanicResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改赛制特殊机制绑定")
	fun update(
		@PathVariable id: Long,
		@RequestBody request: BattleFormatSpecialMechanicRequest,
	): BattleFormatSpecialMechanicResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除赛制特殊机制绑定")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
