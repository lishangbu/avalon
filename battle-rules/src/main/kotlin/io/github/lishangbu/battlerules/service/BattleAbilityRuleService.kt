package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleAbilityRuleRequest
import io.github.lishangbu.battlerules.dto.BattleAbilityRuleResponse
import io.github.lishangbu.battlerules.entity.BattleAbilityRule
import io.github.lishangbu.battlerules.entity.abilityId
import io.github.lishangbu.battlerules.entity.description
import io.github.lishangbu.battlerules.entity.effectPolicy
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.triggerOrder
import io.github.lishangbu.battlerules.entity.triggerTiming
import io.github.lishangbu.battlerules.repository.BattleAbilityRuleRepository
import io.github.lishangbu.common.web.conflict
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.ne
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 战斗特性规则维护服务。
 *
 * 特性往往是现代战斗里最容易膨胀的规则来源：同一特性可能修改伤害、免疫效果、入场触发、回合结束触发，
 * 甚至改变其它规则的优先级。这个服务只管理“触发点到效果策略”的声明，不在资料维护层实现特性逻辑。
 *
 * `triggerTiming` 约束为大写稳定文本，`effectPolicy` 约束为小写策略编码。这样的组合既便于后台维护，
 * 也便于引擎在启动或缓存刷新时把规则解析成强类型处理器。
 */
@Service
class BattleAbilityRuleService(
	private val repository: BattleAbilityRuleRepository,
	private val sqlClient: KSqlClient,
) {
	/**
	 * 分页查询特性规则。
	 */
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, abilityId: Long?, triggerTiming: String?, query: String?): Page<BattleAbilityRuleResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(BattleAbilityRule::class) {
			abilityId?.let { where(table.abilityId eq requiredPositiveId(it, "abilityId")) }
			triggerTiming?.takeIf { it.isNotBlank() }?.let { where(table.triggerTiming eq requiredUpperText(it, "triggerTiming", 60)) }
			search.pattern?.let { pattern -> where(table.effectPolicy ilike pattern) }
			orderBy(table.sortOrder, table.abilityId, table.triggerOrder)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	/**
	 * 读取单条特性规则。
	 */
	@Transactional(readOnly = true)
	fun get(id: Long): BattleAbilityRuleResponse =
		ruleByIdOrNotFound(id).toResponse()

	/**
	 * 创建特性规则。
	 */
	@Transactional
	fun create(request: BattleAbilityRuleRequest): BattleAbilityRuleResponse {
		val normalized = request.normalized()
		validateAbilityReference(normalized.abilityId)
		ensureRuleAvailable(normalized, null)
		return repository.save(
			BattleAbilityRule {
				abilityId = normalized.abilityId
				triggerTiming = normalized.triggerTiming
				effectPolicy = normalized.effectPolicy
				triggerOrder = normalized.triggerOrder
				description = normalized.description
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 更新特性规则。
	 */
	@Transactional
	fun update(id: Long, request: BattleAbilityRuleRequest): BattleAbilityRuleResponse {
		ruleByIdOrNotFound(id)
		val normalized = request.normalized()
		validateAbilityReference(normalized.abilityId)
		ensureRuleAvailable(normalized, id)
		return repository.save(
			BattleAbilityRule {
				this.id = id
				abilityId = normalized.abilityId
				triggerTiming = normalized.triggerTiming
				effectPolicy = normalized.effectPolicy
				triggerOrder = normalized.triggerOrder
				description = normalized.description
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 删除特性规则。
	 */
	@Transactional
	fun delete(id: Long) {
		ruleByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun ruleByIdOrNotFound(id: Long): BattleAbilityRule =
		repository.findNullable(id) ?: notFound("id", "特性规则不存在: $id")

	private fun validateAbilityReference(abilityId: Long) {
		requireExistingGameAbilityReference(sqlClient, abilityId, "abilityId", "特性")
	}

	private fun ensureRuleAvailable(request: BattleAbilityRuleRequest, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleAbilityRule::class) {
			where(table.abilityId eq request.abilityId)
			where(table.triggerTiming eq request.triggerTiming)
			where(table.effectPolicy eq request.effectPolicy)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("effectPolicy", "该特性在同一触发时机下已经配置相同效果策略")
		}
	}

	private fun BattleAbilityRuleRequest.normalized(): BattleAbilityRuleRequest =
		copy(
			abilityId = requiredPositiveId(abilityId, "abilityId"),
			triggerTiming = requiredUpperText(triggerTiming, "triggerTiming", 60),
			effectPolicy = requiredPolicyCode(effectPolicy, "effectPolicy"),
			triggerOrder = requiredIntRange(triggerOrder, "triggerOrder", 0, 10000),
			description = optionalText(description, "description", 800),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleAbilityRule.toResponse(): BattleAbilityRuleResponse =
		BattleAbilityRuleResponse {
			id = this@toResponse.id
			abilityId = this@toResponse.abilityId
			triggerTiming = this@toResponse.triggerTiming
			effectPolicy = this@toResponse.effectPolicy
			triggerOrder = this@toResponse.triggerOrder
			description = this@toResponse.description
			enabled = this@toResponse.enabled
			sortOrder = this@toResponse.sortOrder
		}
}
