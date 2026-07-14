package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.common.web.security.RequireBattleRulesAdmin

import io.github.lishangbu.battlerules.dto.BattleFieldRuleRequest
import io.github.lishangbu.battlerules.dto.BattleFieldRuleResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleFieldRuleService
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
 * 战斗场上效果规则管理 API。
 */
@RequireBattleRulesAdmin
@RestController
@RequestMapping("/api/battle-rules/field-rules")
@Tag(name = "战斗规则 - 场上效果")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleFieldRuleController(
	private val service: BattleFieldRuleService,
) {
	@GetMapping
	@Operation(summary = "分页查询场上效果规则")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) q: String?,
	): Page<BattleFieldRuleResponse> =
		service.list(page, size, q)

	@GetMapping("/{id}")
	@Operation(summary = "读取场上效果规则")
	fun get(@PathVariable id: Long): BattleFieldRuleResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增场上效果规则")
	fun create(@RequestBody request: BattleFieldRuleRequest): BattleFieldRuleResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改场上效果规则")
	fun update(@PathVariable id: Long, @RequestBody request: BattleFieldRuleRequest): BattleFieldRuleResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除场上效果规则")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
