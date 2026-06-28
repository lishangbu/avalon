package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.battlerules.dto.BattleRuleCoverageResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleRuleCoverageService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 战斗规则覆盖报告 API。
 *
 * 覆盖报告是代码实现和公开 fixture 的只读视图，不提供 CRUD，也不把测试覆盖状态写入规则资料表。
 */
@RestController
@RequestMapping("/api/battle-rules/coverage")
@Tag(name = "战斗规则 - 覆盖报告")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleRuleCoverageController(
	private val service: BattleRuleCoverageService,
) {
	@GetMapping
	@Operation(summary = "读取战斗规则覆盖报告")
	fun getCoverage(): BattleRuleCoverageResponse =
		service.getCoverage()
}
