package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.common.web.security.RequireBattleRulesAdmin

import io.github.lishangbu.battlerules.dto.BattleSkillStatStageOperationRequest
import io.github.lishangbu.battlerules.dto.BattleSkillStatStageOperationResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleSkillStatStageOperationService
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
 * 技能能力阶级操作管理 API。
 *
 * 该控制器维护清除、复制、交换和取反能力阶级等技能规则。它与普通能力阶级加减效果分开维护，
 * 让资料页和运行时都能区分“加减几级”和“按当前战斗状态重写阶级”两类规则。
 */
@RequireBattleRulesAdmin
@RestController
@RequestMapping("/api/battle-rules/skill-stat-stage-operations")
@Tag(name = "战斗规则 - 技能能力阶级操作")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleSkillStatStageOperationController(
	private val service: BattleSkillStatStageOperationService,
) {
	@GetMapping
	@Operation(summary = "分页查询技能能力阶级操作")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) skillRuleId: Long?,
		@RequestParam(required = false) statId: Long?,
		@RequestParam(required = false) operationKind: String?,
	): Page<BattleSkillStatStageOperationResponse> =
		service.list(page, size, skillRuleId, statId, operationKind)

	@GetMapping("/{id}")
	@Operation(summary = "读取技能能力阶级操作")
	fun get(@PathVariable id: Long): BattleSkillStatStageOperationResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增技能能力阶级操作")
	fun create(@RequestBody request: BattleSkillStatStageOperationRequest): BattleSkillStatStageOperationResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改技能能力阶级操作")
	fun update(
		@PathVariable id: Long,
		@RequestBody request: BattleSkillStatStageOperationRequest,
	): BattleSkillStatStageOperationResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除技能能力阶级操作")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}

