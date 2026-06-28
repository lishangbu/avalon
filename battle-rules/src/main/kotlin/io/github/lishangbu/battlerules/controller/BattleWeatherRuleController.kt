package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.battlerules.dto.BattleWeatherRuleRequest
import io.github.lishangbu.battlerules.dto.BattleWeatherRuleResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleWeatherRuleService
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
 * 战斗天气规则管理 API。
 */
@RestController
@RequestMapping("/api/battle-rules/weather-rules")
@Tag(name = "战斗规则 - 天气规则")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleWeatherRuleController(
	private val service: BattleWeatherRuleService,
) {
	@GetMapping
	@Operation(summary = "分页查询天气规则")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) q: String?,
	): Page<BattleWeatherRuleResponse> =
		service.list(page, size, q)

	@GetMapping("/{id}")
	@Operation(summary = "读取天气规则")
	fun get(@PathVariable id: Long): BattleWeatherRuleResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增天气规则")
	fun create(@RequestBody request: BattleWeatherRuleRequest): BattleWeatherRuleResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改天气规则")
	fun update(@PathVariable id: Long, @RequestBody request: BattleWeatherRuleRequest): BattleWeatherRuleResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除天气规则")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
