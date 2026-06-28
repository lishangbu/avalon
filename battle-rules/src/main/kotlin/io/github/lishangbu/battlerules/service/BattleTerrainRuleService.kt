package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleTerrainRuleRequest
import io.github.lishangbu.battlerules.dto.BattleTerrainRuleResponse
import io.github.lishangbu.battlerules.entity.BattleTerrainRule
import io.github.lishangbu.battlerules.entity.code
import io.github.lishangbu.battlerules.entity.defaultDurationTurns
import io.github.lishangbu.battlerules.entity.description
import io.github.lishangbu.battlerules.entity.effectPolicy
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.name
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.repository.BattleTerrainRuleRepository
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
 * 战斗场地规则维护服务。
 *
 * 场地规则通常带有“只影响接触地面的成员”这类运行时判断。数据库只保存策略入口和持续回合，
 * 不把站位判断或免疫逻辑做成自由文本配置。
 */
@Service
class BattleTerrainRuleService(
	private val repository: BattleTerrainRuleRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, query: String?): Page<BattleTerrainRuleResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(BattleTerrainRule::class) {
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern))
			}
			orderBy(table.sortOrder, table.code)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): BattleTerrainRuleResponse =
		ruleByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: BattleTerrainRuleRequest): BattleTerrainRuleResponse {
		val code = request.code.requiredSlugCode("code")
		ensureCodeAvailable(code, null)
		val normalized = request.normalized()
		return repository.save(
			BattleTerrainRule {
				this.code = code
				name = normalized.name
				effectPolicy = normalized.effectPolicy
				defaultDurationTurns = normalized.defaultDurationTurns
				description = normalized.description
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun update(id: Long, request: BattleTerrainRuleRequest): BattleTerrainRuleResponse {
		ruleByIdOrNotFound(id)
		val code = request.code.requiredSlugCode("code")
		ensureCodeAvailable(code, id)
		val normalized = request.normalized()
		return repository.save(
			BattleTerrainRule {
				this.id = id
				this.code = code
				name = normalized.name
				effectPolicy = normalized.effectPolicy
				defaultDurationTurns = normalized.defaultDurationTurns
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

	private fun ruleByIdOrNotFound(id: Long): BattleTerrainRule =
		repository.findNullable(id) ?: notFound("id", "场地规则不存在: $id")

	private fun ensureCodeAvailable(code: String, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleTerrainRule::class) {
			where(table.code eq code)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("code", "场地规则 code 已存在: $code")
		}
	}

	private fun BattleTerrainRuleRequest.normalized(): BattleTerrainRuleRequest =
		copy(
			name = name.requiredText("name", maxLength = 80),
			effectPolicy = requiredPolicyCode(effectPolicy, "effectPolicy"),
			defaultDurationTurns = optionalIntRange(defaultDurationTurns, "defaultDurationTurns", 1, 99),
			description = optionalText(description, "description", 800),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleTerrainRule.toResponse(): BattleTerrainRuleResponse =
		BattleTerrainRuleResponse(
			id = id,
			code = code,
			name = name,
			effectPolicy = effectPolicy,
			defaultDurationTurns = defaultDurationTurns,
			description = description,
			enabled = enabled,
			sortOrder = sortOrder,
		)
}
