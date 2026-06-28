package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.battlerules.dto.BattleItemRuleRequest
import io.github.lishangbu.battlerules.dto.BattleItemRuleResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_RULES_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleItemRuleService
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
 * 战斗道具规则管理 API。
 *
 * 该控制器维护道具在战斗中何时触发、执行何种策略以及触发后是否消耗。
 * 它只关心战斗规则，不管理背包、库存、价格或基础道具分类。
 */
@RestController
@RequestMapping("/api/battle-rules/item-rules")
@Tag(name = "战斗规则 - 道具规则")
@SecurityRequirement(name = BATTLE_RULES_API_BEARER_AUTH)
class BattleItemRuleController(
	private val service: BattleItemRuleService,
) {
	@GetMapping
	@Operation(summary = "分页查询道具规则")
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "50") size: Int,
		@RequestParam(required = false) itemId: Long?,
		@RequestParam(required = false) triggerTiming: String?,
		@RequestParam(required = false) q: String?,
	): Page<BattleItemRuleResponse> =
		service.list(page, size, itemId, triggerTiming, q)

	@GetMapping("/{id}")
	@Operation(summary = "读取道具规则")
	fun get(@PathVariable id: Long): BattleItemRuleResponse =
		service.get(id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "新增道具规则")
	fun create(@RequestBody request: BattleItemRuleRequest): BattleItemRuleResponse =
		service.create(request)

	@PutMapping("/{id}")
	@Operation(summary = "修改道具规则")
	fun update(@PathVariable id: Long, @RequestBody request: BattleItemRuleRequest): BattleItemRuleResponse =
		service.update(id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除道具规则")
	fun delete(@PathVariable id: Long) {
		service.delete(id)
	}
}
