package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.common.web.security.RequireBattleRulesAdmin

import io.github.lishangbu.battlerules.dto.BattleAbilityRuleRequest
import io.github.lishangbu.battlerules.dto.BattleAbilityRuleResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleAbilityRuleService
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
 * 战斗特性规则管理 API。
 *
 * 该控制器维护特性在战斗中的触发时机和效果策略。
 * 同一特性可以拥有多条规则，因此这里按规则记录独立增删改查，而不是把所有配置折叠成一个自由文本字段。
 */
@RequireBattleRulesAdmin
@RestController
@RequestMapping("/api/battle-rules/ability-rules")
@Tag(name = "战斗规则 - 特性规则")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleAbilityRuleController(
	private val service: BattleAbilityRuleService,
) {
	@GetMapping
	@Operation(summary = "分页查询特性规则")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) abilityId: Long?,
		@RequestParam(required = false) triggerTiming: String?,
		@RequestParam(required = false) q: String?,
	): Page<BattleAbilityRuleResponse> =
		service.list(page, size, abilityId, triggerTiming, q)

	@GetMapping("/{id}")
	@Operation(summary = "读取特性规则")
	fun get(@PathVariable id: Long): BattleAbilityRuleResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增特性规则")
	fun create(@RequestBody request: BattleAbilityRuleRequest): BattleAbilityRuleResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改特性规则")
	fun update(@PathVariable id: Long, @RequestBody request: BattleAbilityRuleRequest): BattleAbilityRuleResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除特性规则")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
