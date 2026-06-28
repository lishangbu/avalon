package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.battlerules.dto.BattleSkillFieldEffectRequest
import io.github.lishangbu.battlerules.dto.BattleSkillFieldEffectResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleSkillFieldEffectService
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
 * 技能场上效果管理 API。
 *
 * 该控制器维护技能命中后建立一侧场上效果的规则关系，例如使用者一侧获得防守屏障。
 * 它不复用通用资料表服务，每个字段和筛选条件都对应本资源的明确业务语义。
 */
@RestController
@RequestMapping("/api/battle-rules/skill-field-effects")
@Tag(name = "战斗规则 - 技能场上效果")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleSkillFieldEffectController(
	private val service: BattleSkillFieldEffectService,
) {
	@GetMapping
	@Operation(summary = "分页查询技能场上效果")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) skillRuleId: Long?,
		@RequestParam(required = false) fieldRuleId: Long?,
	): Page<BattleSkillFieldEffectResponse> =
		service.list(page, size, skillRuleId, fieldRuleId)

	@GetMapping("/{id}")
	@Operation(summary = "读取技能场上效果")
	fun get(@PathVariable id: Long): BattleSkillFieldEffectResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增技能场上效果")
	fun create(@RequestBody request: BattleSkillFieldEffectRequest): BattleSkillFieldEffectResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改技能场上效果")
	fun update(
		@PathVariable id: Long,
		@RequestBody request: BattleSkillFieldEffectRequest,
	): BattleSkillFieldEffectResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除技能场上效果")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
