package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.battlerules.dto.BattleRuleTestRunRequest
import io.github.lishangbu.battlerules.dto.BattleRuleTestRunResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleRuleTestRunService
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
 * 战斗规则 Fixture 测试运行结果管理 API。
 */
@RestController
@RequestMapping("/api/battle-rules/test-runs")
@Tag(name = "战斗规则 - 测试运行结果")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleRuleTestRunController(
	private val service: BattleRuleTestRunService,
) {
	@GetMapping
	@Operation(summary = "分页查询测试运行结果")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) fixtureId: Long?,
		@RequestParam(required = false) runStatus: String?,
	): Page<BattleRuleTestRunResponse> =
		service.list(page, size, fixtureId, runStatus)

	@GetMapping("/{id}")
	@Operation(summary = "读取测试运行结果")
	fun get(@PathVariable id: Long): BattleRuleTestRunResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增测试运行结果")
	fun create(@RequestBody request: BattleRuleTestRunRequest): BattleRuleTestRunResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改测试运行结果")
	fun update(@PathVariable id: Long, @RequestBody request: BattleRuleTestRunRequest): BattleRuleTestRunResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除测试运行结果")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
