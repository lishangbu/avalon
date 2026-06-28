package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleFieldRuleRequest
import io.github.lishangbu.battlerules.dto.BattleFieldRuleResponse
import io.github.lishangbu.battlerules.entity.BattleFieldRule
import io.github.lishangbu.battlerules.entity.code
import io.github.lishangbu.battlerules.entity.description
import io.github.lishangbu.battlerules.entity.effectPolicy
import io.github.lishangbu.battlerules.entity.effectScope
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.maxLayers
import io.github.lishangbu.battlerules.entity.maxTurns
import io.github.lishangbu.battlerules.entity.minTurns
import io.github.lishangbu.battlerules.entity.name
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.repository.BattleFieldRuleRepository
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
 * 战斗场上效果规则维护服务。
 *
 * 场上效果的作用域和叠加层数会直接影响引擎状态容器设计，因此这里把 `effectScope` 和 `maxLayers`
 * 当作结构化字段校验，而不是依赖说明文本让实现类自行猜测。
 */
@Service
class BattleFieldRuleService(
	private val repository: BattleFieldRuleRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, query: String?): Page<BattleFieldRuleResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(BattleFieldRule::class) {
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern))
			}
			orderBy(table.sortOrder, table.code)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): BattleFieldRuleResponse =
		ruleByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: BattleFieldRuleRequest): BattleFieldRuleResponse {
		val code = request.code.requiredSlugCode("code")
		ensureCodeAvailable(code, null)
		val normalized = request.normalized()
		return repository.save(
			BattleFieldRule {
				this.code = code
				name = normalized.name
				effectScope = normalized.effectScope
				effectPolicy = normalized.effectPolicy
				minTurns = normalized.minTurns
				maxTurns = normalized.maxTurns
				maxLayers = normalized.maxLayers
				description = normalized.description
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun update(id: Long, request: BattleFieldRuleRequest): BattleFieldRuleResponse {
		ruleByIdOrNotFound(id)
		val code = request.code.requiredSlugCode("code")
		ensureCodeAvailable(code, id)
		val normalized = request.normalized()
		return repository.save(
			BattleFieldRule {
				this.id = id
				this.code = code
				name = normalized.name
				effectScope = normalized.effectScope
				effectPolicy = normalized.effectPolicy
				minTurns = normalized.minTurns
				maxTurns = normalized.maxTurns
				maxLayers = normalized.maxLayers
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

	private fun ruleByIdOrNotFound(id: Long): BattleFieldRule =
		repository.findNullable(id) ?: notFound("id", "场上效果规则不存在: $id")

	private fun ensureCodeAvailable(code: String, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleFieldRule::class) {
			where(table.code eq code)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("code", "场上效果规则 code 已存在: $code")
		}
	}

	private fun BattleFieldRuleRequest.normalized(): BattleFieldRuleRequest =
		copy(
			name = name.requiredText("name", maxLength = 80),
			effectScope = requiredUpperText(effectScope, "effectScope", 40),
			effectPolicy = requiredPolicyCode(effectPolicy, "effectPolicy"),
			minTurns = optionalIntRange(minTurns, "minTurns", 1, 99),
			maxTurns = optionalIntRange(maxTurns, "maxTurns", 1, 99),
			maxLayers = optionalIntRange(maxLayers, "maxLayers", 1, 10),
			description = optionalText(description, "description", 800),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleFieldRule.toResponse(): BattleFieldRuleResponse =
		BattleFieldRuleResponse(
			id = id,
			code = code,
			name = name,
			effectScope = effectScope,
			effectPolicy = effectPolicy,
			minTurns = minTurns,
			maxTurns = maxTurns,
			maxLayers = maxLayers,
			description = description,
			enabled = enabled,
			sortOrder = sortOrder,
		)
}
