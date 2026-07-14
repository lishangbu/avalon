package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.common.web.security.RequireBattleRulesAdmin

import io.github.lishangbu.battlerules.dto.BattleSkillTerrainPowerModifierRequest
import io.github.lishangbu.battlerules.dto.BattleSkillTerrainPowerModifierResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleSkillTerrainPowerModifierService
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
 * 技能场地威力倍率管理 API。
 *
 * 该控制器维护技能在指定场地下的威力倍率规则。倍率只在使用者接地时进入普通伤害公式的威力阶段，
 * 因此它和场地本身的最终伤害倍率、属性覆盖资料保持独立。
 */
@RequireBattleRulesAdmin
@RestController
@RequestMapping("/api/battle-rules/skill-terrain-power-modifiers")
@Tag(name = "战斗规则 - 技能场地威力倍率")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleSkillTerrainPowerModifierController(
	private val service: BattleSkillTerrainPowerModifierService,
) {
	@GetMapping
	@Operation(summary = "分页查询技能场地威力倍率")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) skillRuleId: Long?,
		@RequestParam(required = false) terrainRuleId: Long?,
	): Page<BattleSkillTerrainPowerModifierResponse> =
		service.list(page, size, skillRuleId, terrainRuleId)

	@GetMapping("/{id}")
	@Operation(summary = "读取技能场地威力倍率")
	fun get(@PathVariable id: Long): BattleSkillTerrainPowerModifierResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增技能场地威力倍率")
	fun create(@RequestBody request: BattleSkillTerrainPowerModifierRequest): BattleSkillTerrainPowerModifierResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改技能场地威力倍率")
	fun update(
		@PathVariable id: Long,
		@RequestBody request: BattleSkillTerrainPowerModifierRequest,
	): BattleSkillTerrainPowerModifierResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除技能场地威力倍率")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
