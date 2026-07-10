package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleWeatherRuleRequest
import io.github.lishangbu.battlerules.dto.BattleWeatherRuleResponse
import io.github.lishangbu.battlerules.entity.BattleWeatherRule
import io.github.lishangbu.battlerules.entity.code
import io.github.lishangbu.battlerules.entity.defaultDurationTurns
import io.github.lishangbu.battlerules.entity.description
import io.github.lishangbu.battlerules.entity.effectPolicy
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.name
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.repository.BattleWeatherRuleRepository
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
 * 战斗天气规则维护服务。
 *
 * 天气规则会被引擎全场状态读取。Service 不在这里实现伤害和回合结算，只保证数据库里的策略 code
 * 和默认持续回合处于可解释范围。
 */
@Service
class BattleWeatherRuleService(
	private val repository: BattleWeatherRuleRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, query: String?): Page<BattleWeatherRuleResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(BattleWeatherRule::class) {
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern))
			}
			orderBy(table.sortOrder, table.code)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): BattleWeatherRuleResponse =
		ruleByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: BattleWeatherRuleRequest): BattleWeatherRuleResponse {
		val code = request.code.requiredSlugCode("code")
		ensureCodeAvailable(code, null)
		val normalized = request.normalized()
		return repository.save(
			BattleWeatherRule {
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
	fun update(id: Long, request: BattleWeatherRuleRequest): BattleWeatherRuleResponse {
		ruleByIdOrNotFound(id)
		val code = request.code.requiredSlugCode("code")
		ensureCodeAvailable(code, id)
		val normalized = request.normalized()
		return repository.save(
			BattleWeatherRule {
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

	private fun ruleByIdOrNotFound(id: Long): BattleWeatherRule =
		repository.findNullable(id) ?: notFound("id", "天气规则不存在: $id")

	private fun ensureCodeAvailable(code: String, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleWeatherRule::class) {
			where(table.code eq code)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("code", "天气规则 code 已存在: $code")
		}
	}

	private fun BattleWeatherRuleRequest.normalized(): BattleWeatherRuleRequest =
		copy(
			name = name.requiredText("name", maxLength = 80),
			effectPolicy = requiredPolicyCode(effectPolicy, "effectPolicy"),
			defaultDurationTurns = optionalIntRange(defaultDurationTurns, "defaultDurationTurns", 1, 99),
			description = optionalText(description, "description", 800),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleWeatherRule.toResponse(): BattleWeatherRuleResponse =
		BattleWeatherRuleResponse {
			id = this@toResponse.id
			code = this@toResponse.code
			name = this@toResponse.name
			effectPolicy = this@toResponse.effectPolicy
			defaultDurationTurns = this@toResponse.defaultDurationTurns
			description = this@toResponse.description
			enabled = this@toResponse.enabled
			sortOrder = this@toResponse.sortOrder
		}
}
