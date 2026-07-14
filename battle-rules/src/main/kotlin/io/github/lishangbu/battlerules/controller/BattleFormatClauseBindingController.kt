package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.common.web.security.RequireBattleRulesAdmin

import io.github.lishangbu.battlerules.dto.BattleFormatClauseBindingRequest
import io.github.lishangbu.battlerules.dto.BattleFormatClauseBindingResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleFormatClauseBindingService
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
 * 战斗赛制条款绑定管理 API。
 */
@RequireBattleRulesAdmin
@RestController
@RequestMapping("/api/battle-rules/format-clause-bindings")
@Tag(name = "战斗规则 - 赛制条款绑定")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleFormatClauseBindingController(
	private val service: BattleFormatClauseBindingService,
) {
	@GetMapping
	@Operation(summary = "分页查询赛制条款绑定")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) formatId: Long?,
		@RequestParam(required = false) clauseId: Long?,
	): Page<BattleFormatClauseBindingResponse> =
		service.list(page, size, formatId, clauseId)

	@GetMapping("/{id}")
	@Operation(summary = "读取赛制条款绑定")
	fun get(@PathVariable id: Long): BattleFormatClauseBindingResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增赛制条款绑定")
	fun create(@RequestBody request: BattleFormatClauseBindingRequest): BattleFormatClauseBindingResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改赛制条款绑定")
	fun update(
		@PathVariable id: Long,
		@RequestBody request: BattleFormatClauseBindingRequest,
	): BattleFormatClauseBindingResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除赛制条款绑定")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
