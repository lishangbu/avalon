package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.common.web.security.RequireBattleRulesAdmin

import io.github.lishangbu.battlerules.dto.BattleSkillTerrainElementOverrideRequest
import io.github.lishangbu.battlerules.dto.BattleSkillTerrainElementOverrideResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleSkillTerrainElementOverrideService
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
 * 技能场地属性覆盖管理 API。
 *
 * 该控制器维护技能在指定场地下改用哪个属性结算，例如场地脉冲在精神场地改为超能力属性。
 * 属性覆盖会被战斗引擎统一用于伤害、属性吸收和属性道具判断，因此它必须独立于威力倍率维护。
 */
@RequireBattleRulesAdmin
@RestController
@RequestMapping("/api/battle-rules/skill-terrain-element-overrides")
@Tag(name = "战斗规则 - 技能场地属性覆盖")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleSkillTerrainElementOverrideController(
	private val service: BattleSkillTerrainElementOverrideService,
) {
	@GetMapping
	@Operation(summary = "分页查询技能场地属性覆盖")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) skillRuleId: Long?,
		@RequestParam(required = false) terrainRuleId: Long?,
		@RequestParam(required = false) targetElementId: Long?,
	): Page<BattleSkillTerrainElementOverrideResponse> =
		service.list(page, size, skillRuleId, terrainRuleId, targetElementId)

	@GetMapping("/{id}")
	@Operation(summary = "读取技能场地属性覆盖")
	fun get(@PathVariable id: Long): BattleSkillTerrainElementOverrideResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增技能场地属性覆盖")
	fun create(@RequestBody request: BattleSkillTerrainElementOverrideRequest): BattleSkillTerrainElementOverrideResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改技能场地属性覆盖")
	fun update(
		@PathVariable id: Long,
		@RequestBody request: BattleSkillTerrainElementOverrideRequest,
	): BattleSkillTerrainElementOverrideResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除技能场地属性覆盖")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
