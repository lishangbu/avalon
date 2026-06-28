package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.battlerules.dto.BattleRuleFixtureRequest
import io.github.lishangbu.battlerules.dto.BattleRuleFixtureResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleRuleFixtureService
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
 * 战斗规则公开对照 Fixture 管理 API。
 */
@RestController
@RequestMapping("/api/battle-rules/fixtures")
@Tag(name = "战斗规则 - 对照 Fixture")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleRuleFixtureController(
	private val service: BattleRuleFixtureService,
) {
	@GetMapping
	@Operation(summary = "分页查询对照 Fixture")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) q: String?,
		@RequestParam(required = false) category: String?,
		@RequestParam(required = false) enabled: Boolean?,
	): Page<BattleRuleFixtureResponse> =
		service.list(page, size, q, category, enabled)

	@GetMapping("/{id}")
	@Operation(summary = "读取对照 Fixture")
	fun get(@PathVariable id: Long): BattleRuleFixtureResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增对照 Fixture")
	fun create(@RequestBody request: BattleRuleFixtureRequest): BattleRuleFixtureResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改对照 Fixture")
	fun update(@PathVariable id: Long, @RequestBody request: BattleRuleFixtureRequest): BattleRuleFixtureResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除对照 Fixture")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
