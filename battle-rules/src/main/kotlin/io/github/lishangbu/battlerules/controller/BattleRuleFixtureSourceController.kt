package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.battlerules.dto.BattleRuleFixtureSourceRequest
import io.github.lishangbu.battlerules.dto.BattleRuleFixtureSourceResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleRuleFixtureSourceService
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
 * 战斗规则 Fixture 公开来源管理 API。
 */
@RestController
@RequestMapping("/api/battle-rules/fixture-sources")
@Tag(name = "战斗规则 - Fixture 来源")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleRuleFixtureSourceController(
	private val service: BattleRuleFixtureSourceService,
) {
	@GetMapping
	@Operation(summary = "分页查询 Fixture 来源")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) fixtureId: Long?,
	): Page<BattleRuleFixtureSourceResponse> =
		service.list(page, size, fixtureId)

	@GetMapping("/{id}")
	@Operation(summary = "读取 Fixture 来源")
	fun get(@PathVariable id: Long): BattleRuleFixtureSourceResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增 Fixture 来源")
	fun create(@RequestBody request: BattleRuleFixtureSourceRequest): BattleRuleFixtureSourceResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改 Fixture 来源")
	fun update(@PathVariable id: Long, @RequestBody request: BattleRuleFixtureSourceRequest): BattleRuleFixtureSourceResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除 Fixture 来源")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
