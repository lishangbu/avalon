package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleSkillStatusEffectRequest
import io.github.lishangbu.battlerules.dto.BattleSkillStatusEffectResponse
import io.github.lishangbu.battlerules.entity.BattleSkillStatusEffect
import io.github.lishangbu.battlerules.entity.chancePercent
import io.github.lishangbu.battlerules.entity.effectTiming
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.skillRuleId
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.statusRuleId
import io.github.lishangbu.battlerules.entity.targetScope
import io.github.lishangbu.battlerules.repository.BattleSkillRuleRepository
import io.github.lishangbu.battlerules.repository.BattleSkillStatusEffectRepository
import io.github.lishangbu.battlerules.repository.BattleStatusRuleRepository
import io.github.lishangbu.common.web.conflict
import io.github.lishangbu.common.web.invalidReference
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.validatePage
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ne
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 技能状态附加效果维护服务。
 *
 * 技能主规则只描述“如何命中和造成伤害”，状态附加效果则单独维护为多值子表。这样一个技能可以同时拥有多个
 * 附加状态，也可以在不影响主规则的情况下调整某个状态的概率、目标范围和结算时机。
 *
 * Service 显式校验技能规则与状态规则存在，并对 `(skillRuleId, statusRuleId, targetScope, effectTiming)` 做判重。
 * 这让规则编辑界面能够把重复配置尽早提示出来，数据库唯一约束则继续承担并发写入时的最终保护。
 */
@Service
class BattleSkillStatusEffectService(
	private val repository: BattleSkillStatusEffectRepository,
	private val skillRuleRepository: BattleSkillRuleRepository,
	private val statusRuleRepository: BattleStatusRuleRepository,
	private val sqlClient: KSqlClient,
) {
	/**
	 * 按技能规则或状态规则分页查询附加状态效果。
	 */
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, skillRuleId: Long?, statusRuleId: Long?): Page<BattleSkillStatusEffectResponse> {
		validatePage(page, size)
		return sqlClient.createQuery(BattleSkillStatusEffect::class) {
			skillRuleId?.let { where(table.skillRuleId eq requiredPositiveId(it, "skillRuleId")) }
			statusRuleId?.let { where(table.statusRuleId eq requiredPositiveId(it, "statusRuleId")) }
			orderBy(table.skillRuleId, table.sortOrder, table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	/**
	 * 读取单条技能状态附加效果。
	 */
	@Transactional(readOnly = true)
	fun get(id: Long): BattleSkillStatusEffectResponse =
		effectByIdOrNotFound(id).toResponse()

	/**
	 * 创建技能状态附加效果。
	 */
	@Transactional
	fun create(request: BattleSkillStatusEffectRequest): BattleSkillStatusEffectResponse {
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.statusRuleId)
		ensureEffectAvailable(normalized, null)
		return repository.save(
			BattleSkillStatusEffect {
				skillRuleId = normalized.skillRuleId
				statusRuleId = normalized.statusRuleId
				targetScope = normalized.targetScope
				effectTiming = normalized.effectTiming
				chancePercent = normalized.chancePercent
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 更新技能状态附加效果。
	 */
	@Transactional
	fun update(id: Long, request: BattleSkillStatusEffectRequest): BattleSkillStatusEffectResponse {
		effectByIdOrNotFound(id)
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.statusRuleId)
		ensureEffectAvailable(normalized, id)
		return repository.save(
			BattleSkillStatusEffect {
				this.id = id
				skillRuleId = normalized.skillRuleId
				statusRuleId = normalized.statusRuleId
				targetScope = normalized.targetScope
				effectTiming = normalized.effectTiming
				chancePercent = normalized.chancePercent
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 删除技能状态附加效果。
	 */
	@Transactional
	fun delete(id: Long) {
		effectByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun effectByIdOrNotFound(id: Long): BattleSkillStatusEffect =
		repository.findNullable(id) ?: notFound("id", "技能状态附加效果不存在: $id")

	private fun validateReferences(skillRuleId: Long, statusRuleId: Long) {
		if (skillRuleRepository.findNullable(skillRuleId) == null) {
			invalidReference("skillRuleId", "技能规则不存在: $skillRuleId")
		}
		if (statusRuleRepository.findNullable(statusRuleId) == null) {
			invalidReference("statusRuleId", "状态规则不存在: $statusRuleId")
		}
	}

	private fun ensureEffectAvailable(request: BattleSkillStatusEffectRequest, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleSkillStatusEffect::class) {
			where(table.skillRuleId eq request.skillRuleId)
			where(table.statusRuleId eq request.statusRuleId)
			where(table.targetScope eq request.targetScope)
			where(table.effectTiming eq request.effectTiming)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("statusRuleId", "该技能规则已经配置相同状态附加效果")
		}
	}

	private fun BattleSkillStatusEffectRequest.normalized(): BattleSkillStatusEffectRequest =
		copy(
			skillRuleId = requiredPositiveId(skillRuleId, "skillRuleId"),
			statusRuleId = requiredPositiveId(statusRuleId, "statusRuleId"),
			targetScope = requiredUpperText(targetScope, "targetScope", 40),
			effectTiming = requiredUpperText(effectTiming, "effectTiming", 40),
			chancePercent = requiredIntRange(chancePercent, "chancePercent", 0, 100),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleSkillStatusEffect.toResponse(): BattleSkillStatusEffectResponse =
		BattleSkillStatusEffectResponse {
			id = this@toResponse.id
			skillRuleId = this@toResponse.skillRuleId
			statusRuleId = this@toResponse.statusRuleId
			targetScope = this@toResponse.targetScope
			effectTiming = this@toResponse.effectTiming
			chancePercent = this@toResponse.chancePercent
			enabled = this@toResponse.enabled
			sortOrder = this@toResponse.sortOrder
		}
}
