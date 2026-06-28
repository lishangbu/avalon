package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.battlerules.dto.BattleSkillStatStageEffectRequest
import io.github.lishangbu.battlerules.dto.BattleSkillStatStageEffectResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleSkillStatStageEffectService
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
 * 技能能力阶级效果管理 API。
 *
 * 该控制器维护技能造成的攻击、防御、速度等能力阶级变化。
 * 数据模型按效果拆分，可以为同一技能配置多个能力变化，后续战斗引擎按这些记录逐条结算。
 */
@RestController
@RequestMapping("/api/battle-rules/skill-stat-stage-effects")
@Tag(name = "战斗规则 - 技能能力阶级效果")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleSkillStatStageEffectController(
	private val service: BattleSkillStatStageEffectService,
) {
	@GetMapping
	@Operation(summary = "分页查询技能能力阶级效果")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) skillRuleId: Long?,
		@RequestParam(required = false) statId: Long?,
	): Page<BattleSkillStatStageEffectResponse> =
		service.list(page, size, skillRuleId, statId)

	@GetMapping("/{id}")
	@Operation(summary = "读取技能能力阶级效果")
	fun get(@PathVariable id: Long): BattleSkillStatStageEffectResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增技能能力阶级效果")
	fun create(@RequestBody request: BattleSkillStatStageEffectRequest): BattleSkillStatStageEffectResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改技能能力阶级效果")
	fun update(
		@PathVariable id: Long,
		@RequestBody request: BattleSkillStatStageEffectRequest,
	): BattleSkillStatStageEffectResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除技能能力阶级效果")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
