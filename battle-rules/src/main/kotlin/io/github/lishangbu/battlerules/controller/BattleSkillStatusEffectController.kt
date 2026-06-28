package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.battlerules.dto.BattleSkillStatusEffectRequest
import io.github.lishangbu.battlerules.dto.BattleSkillStatusEffectResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleSkillStatusEffectService
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
 * 技能状态附加效果管理 API。
 *
 * 该控制器维护技能命中后附加灼伤、麻痹等状态的概率型规则。
 * 它是技能规则的从属资料，但仍提供独立 CRUD，方便管理端拆分页面逐项维护。
 */
@RestController
@RequestMapping("/api/battle-rules/skill-status-effects")
@Tag(name = "战斗规则 - 技能状态效果")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleSkillStatusEffectController(
	private val service: BattleSkillStatusEffectService,
) {
	@GetMapping
	@Operation(summary = "分页查询技能状态效果")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) skillRuleId: Long?,
		@RequestParam(required = false) statusRuleId: Long?,
	): Page<BattleSkillStatusEffectResponse> =
		service.list(page, size, skillRuleId, statusRuleId)

	@GetMapping("/{id}")
	@Operation(summary = "读取技能状态效果")
	fun get(@PathVariable id: Long): BattleSkillStatusEffectResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增技能状态效果")
	fun create(@RequestBody request: BattleSkillStatusEffectRequest): BattleSkillStatusEffectResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改技能状态效果")
	fun update(@PathVariable id: Long, @RequestBody request: BattleSkillStatusEffectRequest): BattleSkillStatusEffectResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除技能状态效果")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
