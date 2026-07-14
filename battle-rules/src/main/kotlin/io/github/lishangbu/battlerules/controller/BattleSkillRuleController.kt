package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.common.web.security.RequireBattleRulesAdmin

import io.github.lishangbu.battlerules.dto.BattleSkillRuleRequest
import io.github.lishangbu.battlerules.dto.BattleSkillRuleResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleSkillRuleService
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
 * 战斗技能规则管理 API。
 *
 * 该控制器暴露技能主规则的独立维护入口，不经过 `game-data` 的通用表服务。
 * 管理端可以在这里维护技能如何选择目标、命中、造成伤害以及触发常见标签。
 */
@RequireBattleRulesAdmin
@RestController
@RequestMapping("/api/battle-rules/skill-rules")
@Tag(name = "战斗规则 - 技能规则")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleSkillRuleController(
	private val service: BattleSkillRuleService,
) {
	@GetMapping
	@Operation(summary = "分页查询技能规则")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) skillId: Long?,
		@RequestParam(required = false) q: String?,
	): Page<BattleSkillRuleResponse> =
		service.list(page, size, skillId, q)

	@GetMapping("/{id}")
	@Operation(summary = "读取技能规则")
	fun get(@PathVariable id: Long): BattleSkillRuleResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增技能规则")
	fun create(@RequestBody request: BattleSkillRuleRequest): BattleSkillRuleResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改技能规则")
	fun update(@PathVariable id: Long, @RequestBody request: BattleSkillRuleRequest): BattleSkillRuleResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除技能规则")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
