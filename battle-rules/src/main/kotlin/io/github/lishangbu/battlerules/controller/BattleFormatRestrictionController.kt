package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.common.web.security.RequireBattleRulesAdmin

import io.github.lishangbu.battlerules.dto.BattleFormatRestrictionRequest
import io.github.lishangbu.battlerules.dto.BattleFormatRestrictionResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleFormatRestrictionService
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
 * 战斗赛制限制管理 API。
 */
@RequireBattleRulesAdmin
@RestController
@RequestMapping("/api/battle-rules/format-restrictions")
@Tag(name = "战斗规则 - 赛制限制")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleFormatRestrictionController(
	private val service: BattleFormatRestrictionService,
) {
	@GetMapping
	@Operation(summary = "分页查询赛制限制")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) formatId: Long?,
	): Page<BattleFormatRestrictionResponse> =
		service.list(page, size, q, formatId)

	@GetMapping("/{id}")
	@Operation(summary = "读取赛制限制")
	fun get(@PathVariable id: Long): BattleFormatRestrictionResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增赛制限制")
	fun create(@RequestBody request: BattleFormatRestrictionRequest): BattleFormatRestrictionResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改赛制限制")
	fun update(@PathVariable id: Long, @RequestBody request: BattleFormatRestrictionRequest): BattleFormatRestrictionResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除赛制限制")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
