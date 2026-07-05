package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleItemRuleRequest
import io.github.lishangbu.battlerules.dto.BattleItemRuleResponse
import io.github.lishangbu.battlerules.entity.BattleItemRule
import io.github.lishangbu.battlerules.entity.consumable
import io.github.lishangbu.battlerules.entity.description
import io.github.lishangbu.battlerules.entity.effectPolicy
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.itemId
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.triggerOrder
import io.github.lishangbu.battlerules.entity.triggerTiming
import io.github.lishangbu.battlerules.repository.BattleItemRuleRepository
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
 * 战斗道具规则维护服务。
 *
 * 携带道具和一次性树果等效果通常在固定战斗时机触发。本服务把“哪个道具、什么时机、执行哪个策略、是否消耗”
 * 拆成结构化字段，供引擎按触发时机批量加载和排序。
 *
 * 这里不处理背包、库存或获得方式，也不把道具规则合并进角色状态。真正的消耗、回复、伤害增幅和锁招等状态变化
 * 都会在战斗引擎事务内完成；资料维护层只负责让规则配置保持唯一、可引用、可解释。
 */
@Service
class BattleItemRuleService(
	private val repository: BattleItemRuleRepository,
	private val sqlClient: KSqlClient,
) {
	/**
	 * 分页查询道具规则。
	 */
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, itemId: Long?, triggerTiming: String?, query: String?): Page<BattleItemRuleResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(BattleItemRule::class) {
			itemId?.let { where(table.itemId eq requiredPositiveId(it, "itemId")) }
			triggerTiming?.takeIf { it.isNotBlank() }?.let { where(table.triggerTiming eq requiredUpperText(it, "triggerTiming", 60)) }
			search.pattern?.let { pattern -> where(table.effectPolicy ilike pattern) }
			orderBy(table.sortOrder, table.itemId, table.triggerOrder)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	/**
	 * 读取单条道具规则。
	 */
	@Transactional(readOnly = true)
	fun get(id: Long): BattleItemRuleResponse =
		ruleByIdOrNotFound(id).toResponse()

	/**
	 * 创建道具规则。
	 */
	@Transactional
	fun create(request: BattleItemRuleRequest): BattleItemRuleResponse {
		val normalized = request.normalized()
		validateItemReference(normalized.itemId)
		ensureRuleAvailable(normalized, null)
		return repository.save(
			BattleItemRule {
				itemId = normalized.itemId
				triggerTiming = normalized.triggerTiming
				effectPolicy = normalized.effectPolicy
				consumable = normalized.consumable
				triggerOrder = normalized.triggerOrder
				description = normalized.description
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 更新道具规则。
	 */
	@Transactional
	fun update(id: Long, request: BattleItemRuleRequest): BattleItemRuleResponse {
		ruleByIdOrNotFound(id)
		val normalized = request.normalized()
		validateItemReference(normalized.itemId)
		ensureRuleAvailable(normalized, id)
		return repository.save(
			BattleItemRule {
				this.id = id
				itemId = normalized.itemId
				triggerTiming = normalized.triggerTiming
				effectPolicy = normalized.effectPolicy
				consumable = normalized.consumable
				triggerOrder = normalized.triggerOrder
				description = normalized.description
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 删除道具规则。
	 */
	@Transactional
	fun delete(id: Long) {
		ruleByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun ruleByIdOrNotFound(id: Long): BattleItemRule =
		repository.findNullable(id) ?: notFound("id", "道具规则不存在: $id")

	private fun validateItemReference(itemId: Long) {
		requireExistingGameDataReference(sqlClient, "game_item", itemId, "itemId", "道具")
	}

	private fun ensureRuleAvailable(request: BattleItemRuleRequest, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleItemRule::class) {
			where(table.itemId eq request.itemId)
			where(table.triggerTiming eq request.triggerTiming)
			where(table.effectPolicy eq request.effectPolicy)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("effectPolicy", "该道具在同一触发时机下已经配置相同效果策略")
		}
	}

	private fun BattleItemRuleRequest.normalized(): BattleItemRuleRequest =
		copy(
			itemId = requiredPositiveId(itemId, "itemId"),
			triggerTiming = requiredUpperText(triggerTiming, "triggerTiming", 60),
			effectPolicy = requiredPolicyCode(effectPolicy, "effectPolicy"),
			triggerOrder = requiredIntRange(triggerOrder, "triggerOrder", 0, 10000),
			description = optionalText(description, "description", 800),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleItemRule.toResponse(): BattleItemRuleResponse =
		BattleItemRuleResponse(
			id = id,
			itemId = itemId,
			triggerTiming = triggerTiming,
			effectPolicy = effectPolicy,
			consumable = consumable,
			triggerOrder = triggerOrder,
			description = description,
			enabled = enabled,
			sortOrder = sortOrder,
		)
}
