package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.battlerules.dto.BattleSkillWeatherAccuracyOverrideRequest
import io.github.lishangbu.battlerules.dto.BattleSkillWeatherAccuracyOverrideResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleSkillWeatherAccuracyOverrideService
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
 * 技能天气命中覆盖管理 API。
 *
 * 该控制器维护技能在指定天气下的命中覆盖规则，例如下雨时必中、大晴天时命中降低。
 * 它是技能规则的从属资料，但仍提供独立 CRUD，方便管理端拆分页面逐项维护。
 */
@RestController
@RequestMapping("/api/battle-rules/skill-weather-accuracy-overrides")
@Tag(name = "战斗规则 - 技能天气命中覆盖")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleSkillWeatherAccuracyOverrideController(
	private val service: BattleSkillWeatherAccuracyOverrideService,
) {
	@GetMapping
	@Operation(summary = "分页查询技能天气命中覆盖")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) skillRuleId: Long?,
		@RequestParam(required = false) weatherRuleId: Long?,
	): Page<BattleSkillWeatherAccuracyOverrideResponse> =
		service.list(page, size, skillRuleId, weatherRuleId)

	@GetMapping("/{id}")
	@Operation(summary = "读取技能天气命中覆盖")
	fun get(@PathVariable id: Long): BattleSkillWeatherAccuracyOverrideResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增技能天气命中覆盖")
	fun create(@RequestBody request: BattleSkillWeatherAccuracyOverrideRequest): BattleSkillWeatherAccuracyOverrideResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改技能天气命中覆盖")
	fun update(
		@PathVariable id: Long,
		@RequestBody request: BattleSkillWeatherAccuracyOverrideRequest,
	): BattleSkillWeatherAccuracyOverrideResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除技能天气命中覆盖")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
