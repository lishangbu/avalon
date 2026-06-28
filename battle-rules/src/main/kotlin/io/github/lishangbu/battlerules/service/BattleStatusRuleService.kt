package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleStatusRuleRequest
import io.github.lishangbu.battlerules.dto.BattleStatusRuleResponse
import io.github.lishangbu.battlerules.entity.BattleStatusRule
import io.github.lishangbu.battlerules.entity.code
import io.github.lishangbu.battlerules.entity.description
import io.github.lishangbu.battlerules.entity.effectPolicy
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.maxTurns
import io.github.lishangbu.battlerules.entity.minTurns
import io.github.lishangbu.battlerules.entity.name
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.statusKind
import io.github.lishangbu.battlerules.repository.BattleStatusRuleRepository
import io.github.lishangbu.common.web.conflict
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.requiredText
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.ne
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 战斗状态规则维护服务。
 *
 * 状态规则会进入战斗引擎状态机，字段校验比普通字典更严格：`effectPolicy` 必须是稳定策略 code，
 * 持续回合必须在合理范围内，避免管理端写入无法被引擎解释的自由文本。
 */
@Service
class BattleStatusRuleService(
	private val repository: BattleStatusRuleRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, query: String?): Page<BattleStatusRuleResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(BattleStatusRule::class) {
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern))
			}
			orderBy(table.sortOrder, table.code)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): BattleStatusRuleResponse =
		ruleByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: BattleStatusRuleRequest): BattleStatusRuleResponse {
		val code = request.code.requiredSlugCode("code")
		ensureCodeAvailable(code, null)
		val normalized = request.normalized()
		return repository.save(
			BattleStatusRule {
				this.code = code
				name = normalized.name
				statusKind = normalized.statusKind
				effectPolicy = normalized.effectPolicy
				minTurns = normalized.minTurns
				maxTurns = normalized.maxTurns
				description = normalized.description
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun update(id: Long, request: BattleStatusRuleRequest): BattleStatusRuleResponse {
		ruleByIdOrNotFound(id)
		val code = request.code.requiredSlugCode("code")
		ensureCodeAvailable(code, id)
		val normalized = request.normalized()
		return repository.save(
			BattleStatusRule {
				this.id = id
				this.code = code
				name = normalized.name
				statusKind = normalized.statusKind
				effectPolicy = normalized.effectPolicy
				minTurns = normalized.minTurns
				maxTurns = normalized.maxTurns
				description = normalized.description
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		ruleByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun ruleByIdOrNotFound(id: Long): BattleStatusRule =
		repository.findNullable(id) ?: notFound("id", "状态规则不存在: $id")

	private fun ensureCodeAvailable(code: String, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleStatusRule::class) {
			where(table.code eq code)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("code", "状态规则 code 已存在: $code")
		}
	}

	private fun BattleStatusRuleRequest.normalized(): BattleStatusRuleRequest =
		copy(
			name = name.requiredText("name", maxLength = 80),
			statusKind = requiredUpperText(statusKind, "statusKind", 40),
			effectPolicy = requiredPolicyCode(effectPolicy, "effectPolicy"),
			minTurns = optionalIntRange(minTurns, "minTurns", 1, 99),
			maxTurns = optionalIntRange(maxTurns, "maxTurns", 1, 99),
			description = optionalText(description, "description", 800),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleStatusRule.toResponse(): BattleStatusRuleResponse =
		BattleStatusRuleResponse(
			id = id,
			code = code,
			name = name,
			statusKind = statusKind,
			effectPolicy = effectPolicy,
			minTurns = minTurns,
			maxTurns = maxTurns,
			description = description,
			enabled = enabled,
			sortOrder = sortOrder,
		)
}
