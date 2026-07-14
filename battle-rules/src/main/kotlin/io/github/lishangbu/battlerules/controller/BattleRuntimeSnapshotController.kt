package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.common.web.security.RequireBattleRulesAdmin

import io.github.lishangbu.battlerules.dto.BattleActionValidationRequest
import io.github.lishangbu.battlerules.dto.BattleActionValidationResponse
import io.github.lishangbu.battlerules.dto.BattlePreparationValidationRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationValidationResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleRuntimeSnapshot
import io.github.lishangbu.battlerules.service.BattleRuntimeSnapshotService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 战斗运行时规则快照 API。
 *
 * 该控制器提供战斗启动前需要的只读规则装配和准备阶段校验，不承担规则资料 CRUD。
 */
@RequireBattleRulesAdmin
@RestController
@RequestMapping("/api/battle-rules/runtime")
@Tag(name = "战斗规则 - 运行时快照")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleRuntimeSnapshotController(
	private val service: BattleRuntimeSnapshotService,
) {
	@GetMapping("/formats/{formatCode}")
	@Operation(summary = "读取赛制运行时快照")
	fun getByFormatCode(@PathVariable formatCode: String): BattleRuntimeSnapshot =
		service.getByFormatCode(formatCode)

	@PostMapping("/preparation-validation")
	@Operation(summary = "校验战斗准备阶段队伍")
	fun validatePreparation(@RequestBody request: BattlePreparationValidationRequest): BattlePreparationValidationResponse =
		service.validatePreparation(request)

	@PostMapping("/action-validation")
	@Operation(summary = "校验战斗首回合行动")
	fun validateActions(@RequestBody request: BattleActionValidationRequest): BattleActionValidationResponse =
		service.validateActions(request)
}
