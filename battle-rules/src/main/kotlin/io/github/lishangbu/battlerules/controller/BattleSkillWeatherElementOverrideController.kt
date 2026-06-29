package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.battlerules.dto.BattleSkillWeatherElementOverrideRequest
import io.github.lishangbu.battlerules.dto.BattleSkillWeatherElementOverrideResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleSkillWeatherElementOverrideService
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
 * 技能天气属性覆盖管理 API。
 *
 * 该控制器维护技能在指定天气下改用哪个属性结算，例如气象球在雨天改为水属性。它是技能规则的从属资料，
 * 但仍提供独立 CRUD，方便管理端按普通表格拆分维护。
 */
@RestController
@RequestMapping("/api/battle-rules/skill-weather-element-overrides")
@Tag(name = "战斗规则 - 技能天气属性覆盖")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleSkillWeatherElementOverrideController(
	private val service: BattleSkillWeatherElementOverrideService,
) {
	@GetMapping
	@Operation(summary = "分页查询技能天气属性覆盖")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) skillRuleId: Long?,
		@RequestParam(required = false) weatherRuleId: Long?,
		@RequestParam(required = false) targetElementId: Long?,
	): Page<BattleSkillWeatherElementOverrideResponse> =
		service.list(page, size, skillRuleId, weatherRuleId, targetElementId)

	@GetMapping("/{id}")
	@Operation(summary = "读取技能天气属性覆盖")
	fun get(@PathVariable id: Long): BattleSkillWeatherElementOverrideResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增技能天气属性覆盖")
	fun create(@RequestBody request: BattleSkillWeatherElementOverrideRequest): BattleSkillWeatherElementOverrideResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改技能天气属性覆盖")
	fun update(
		@PathVariable id: Long,
		@RequestBody request: BattleSkillWeatherElementOverrideRequest,
	): BattleSkillWeatherElementOverrideResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除技能天气属性覆盖")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
