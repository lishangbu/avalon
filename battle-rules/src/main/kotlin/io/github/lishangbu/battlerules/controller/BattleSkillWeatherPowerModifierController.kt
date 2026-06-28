package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.battlerules.dto.BattleSkillWeatherPowerModifierRequest
import io.github.lishangbu.battlerules.dto.BattleSkillWeatherPowerModifierResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleSkillWeatherPowerModifierService
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
 * 技能天气威力倍率管理 API。
 *
 * 该控制器维护技能在指定天气下的威力倍率规则，例如部分天气下威力减半或翻倍。
 * 它是技能规则的从属资料，但仍提供独立 CRUD，方便管理端拆分页面逐项维护。
 */
@RestController
@RequestMapping("/api/battle-rules/skill-weather-power-modifiers")
@Tag(name = "战斗规则 - 技能天气威力倍率")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleSkillWeatherPowerModifierController(
	private val service: BattleSkillWeatherPowerModifierService,
) {
	@GetMapping
	@Operation(summary = "分页查询技能天气威力倍率")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) skillRuleId: Long?,
		@RequestParam(required = false) weatherRuleId: Long?,
	): Page<BattleSkillWeatherPowerModifierResponse> =
		service.list(page, size, skillRuleId, weatherRuleId)

	@GetMapping("/{id}")
	@Operation(summary = "读取技能天气威力倍率")
	fun get(@PathVariable id: Long): BattleSkillWeatherPowerModifierResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增技能天气威力倍率")
	fun create(@RequestBody request: BattleSkillWeatherPowerModifierRequest): BattleSkillWeatherPowerModifierResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改技能天气威力倍率")
	fun update(
		@PathVariable id: Long,
		@RequestBody request: BattleSkillWeatherPowerModifierRequest,
	): BattleSkillWeatherPowerModifierResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除技能天气威力倍率")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
