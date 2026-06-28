package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleSkillStatStageEffectRequest
import io.github.lishangbu.battlerules.dto.BattleSkillStatStageEffectResponse
import io.github.lishangbu.battlerules.entity.BattleSkillStatStageEffect
import io.github.lishangbu.battlerules.entity.chancePercent
import io.github.lishangbu.battlerules.entity.effectTiming
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.skillRuleId
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.stageDelta
import io.github.lishangbu.battlerules.entity.statId
import io.github.lishangbu.battlerules.entity.targetScope
import io.github.lishangbu.battlerules.repository.BattleSkillRuleRepository
import io.github.lishangbu.battlerules.repository.BattleSkillStatStageEffectRepository
import io.github.lishangbu.common.web.conflict
import io.github.lishangbu.common.web.invalidReference
import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.validatePage
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ne
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 技能能力阶级效果维护服务。
 *
 * 能力阶级变化是战斗引擎的核心副作用之一：它可能发生在命中后、伤害前、使用者自身或所有对手身上。
 * 本服务把这些可维护维度拆成明确字段，而不是把整段规则描述塞进说明文本。
 *
 * 这里特别限制 `stageDelta` 不能为 0，并保持在 -6 到 6 之间。完整战斗状态机会再根据当前成员已有阶级
 * 进行上下限夹取；资料维护层只负责保证“这条效果本身确实表达了一次变化”。
 */
@Service
class BattleSkillStatStageEffectService(
	private val repository: BattleSkillStatStageEffectRepository,
	private val skillRuleRepository: BattleSkillRuleRepository,
	private val sqlClient: KSqlClient,
	private val jdbcTemplate: JdbcTemplate,
) {
	/**
	 * 按技能规则或能力项分页查询阶级效果。
	 */
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, skillRuleId: Long?, statId: Long?): Page<BattleSkillStatStageEffectResponse> {
		validatePage(page, size)
		return sqlClient.createQuery(BattleSkillStatStageEffect::class) {
			skillRuleId?.let { where(table.skillRuleId eq requiredPositiveId(it, "skillRuleId")) }
			statId?.let { where(table.statId eq requiredPositiveId(it, "statId")) }
			orderBy(table.skillRuleId, table.sortOrder, table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	/**
	 * 读取单条技能能力阶级效果。
	 */
	@Transactional(readOnly = true)
	fun get(id: Long): BattleSkillStatStageEffectResponse =
		effectByIdOrNotFound(id).toResponse()

	/**
	 * 创建技能能力阶级效果。
	 */
	@Transactional
	fun create(request: BattleSkillStatStageEffectRequest): BattleSkillStatStageEffectResponse {
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.statId)
		ensureEffectAvailable(normalized, null)
		return repository.save(
			BattleSkillStatStageEffect {
				skillRuleId = normalized.skillRuleId
				statId = normalized.statId
				targetScope = normalized.targetScope
				effectTiming = normalized.effectTiming
				stageDelta = normalized.stageDelta
				chancePercent = normalized.chancePercent
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 更新技能能力阶级效果。
	 */
	@Transactional
	fun update(id: Long, request: BattleSkillStatStageEffectRequest): BattleSkillStatStageEffectResponse {
		effectByIdOrNotFound(id)
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.statId)
		ensureEffectAvailable(normalized, id)
		return repository.save(
			BattleSkillStatStageEffect {
				this.id = id
				skillRuleId = normalized.skillRuleId
				statId = normalized.statId
				targetScope = normalized.targetScope
				effectTiming = normalized.effectTiming
				stageDelta = normalized.stageDelta
				chancePercent = normalized.chancePercent
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 删除技能能力阶级效果。
	 */
	@Transactional
	fun delete(id: Long) {
		effectByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun effectByIdOrNotFound(id: Long): BattleSkillStatStageEffect =
		repository.findNullable(id) ?: notFound("id", "技能能力阶级效果不存在: $id")

	private fun validateReferences(skillRuleId: Long, statId: Long) {
		if (skillRuleRepository.findNullable(skillRuleId) == null) {
			invalidReference("skillRuleId", "技能规则不存在: $skillRuleId")
		}
		requireExistingGameDataReference(jdbcTemplate, "game_stat", statId, "statId", "能力项")
	}

	private fun ensureEffectAvailable(request: BattleSkillStatStageEffectRequest, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleSkillStatStageEffect::class) {
			where(table.skillRuleId eq request.skillRuleId)
			where(table.statId eq request.statId)
			where(table.targetScope eq request.targetScope)
			where(table.effectTiming eq request.effectTiming)
			where(table.stageDelta eq request.stageDelta)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("statId", "该技能规则已经配置相同能力阶级效果")
		}
	}

	private fun BattleSkillStatStageEffectRequest.normalized(): BattleSkillStatStageEffectRequest {
		val normalizedStageDelta = requiredIntRange(stageDelta, "stageDelta", -6, 6)
		if (normalizedStageDelta == 0) {
			invalidValue("stageDelta", "stageDelta 不能为 0")
		}
		return copy(
			skillRuleId = requiredPositiveId(skillRuleId, "skillRuleId"),
			statId = requiredPositiveId(statId, "statId"),
			targetScope = requiredUpperText(targetScope, "targetScope", 40),
			effectTiming = requiredUpperText(effectTiming, "effectTiming", 40),
			stageDelta = normalizedStageDelta,
			chancePercent = requiredIntRange(chancePercent, "chancePercent", 0, 100),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)
	}

	private fun BattleSkillStatStageEffect.toResponse(): BattleSkillStatStageEffectResponse =
		BattleSkillStatStageEffectResponse(
			id = id,
			skillRuleId = skillRuleId,
			statId = statId,
			targetScope = targetScope,
			effectTiming = effectTiming,
			stageDelta = stageDelta,
			chancePercent = chancePercent,
			enabled = enabled,
			sortOrder = sortOrder,
		)
}
