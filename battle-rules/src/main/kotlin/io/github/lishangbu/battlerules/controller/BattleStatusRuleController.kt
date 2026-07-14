package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.common.web.security.RequireBattleRulesAdmin

import io.github.lishangbu.battlerules.dto.BattleStatusRuleRequest
import io.github.lishangbu.battlerules.dto.BattleStatusRuleResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleStatusRuleService
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
 * 战斗状态规则管理 API。
 */
@RequireBattleRulesAdmin
@RestController
@RequestMapping("/api/battle-rules/status-rules")
@Tag(name = "战斗规则 - 状态规则")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleStatusRuleController(
	private val service: BattleStatusRuleService,
) {
	@GetMapping
	@Operation(summary = "分页查询状态规则")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) q: String?,
	): Page<BattleStatusRuleResponse> =
		service.list(page, size, q)

	@GetMapping("/{id}")
	@Operation(summary = "读取状态规则")
	fun get(@PathVariable id: Long): BattleStatusRuleResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增状态规则")
	fun create(@RequestBody request: BattleStatusRuleRequest): BattleStatusRuleResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改状态规则")
	fun update(@PathVariable id: Long, @RequestBody request: BattleStatusRuleRequest): BattleStatusRuleResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除状态规则")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
