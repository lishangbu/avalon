package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleSkillTerrainPowerModifierRequest
import io.github.lishangbu.battlerules.dto.BattleSkillTerrainPowerModifierResponse
import io.github.lishangbu.battlerules.entity.BattleSkillTerrainPowerModifier
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.powerMultiplier
import io.github.lishangbu.battlerules.entity.skillRuleId
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.terrainRuleId
import io.github.lishangbu.battlerules.repository.BattleSkillRuleRepository
import io.github.lishangbu.battlerules.repository.BattleSkillTerrainPowerModifierRepository
import io.github.lishangbu.battlerules.repository.BattleTerrainRuleRepository
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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 技能场地威力倍率维护服务。
 *
 * 该服务维护“技能规则 + 场地规则 => 威力倍率”的数据关系，典型用途是场地脉冲在使用者接地且处于任意现代场地时
 * 威力翻倍。倍率会进入 battle-engine 的威力阶段，而不是最终伤害倍率阶段；因此它与精神/电气/青草场地提供的
 * 对应属性最终伤害 1.3 倍加成可以自然叠乘。
 *
 * 服务层限制倍率为有限正数，并限制在保守上限内，避免管理端录入 NaN、无穷大或明显不合理的倍率。场地规则必须
 * 是当前引擎枚举支持的现代四场地；若以后新增其它场地，必须先扩展 battle-engine，再允许维护端引用。
 */
@Service
class BattleSkillTerrainPowerModifierService(
	private val repository: BattleSkillTerrainPowerModifierRepository,
	private val skillRuleRepository: BattleSkillRuleRepository,
	private val terrainRuleRepository: BattleTerrainRuleRepository,
	private val sqlClient: KSqlClient,
) {
	/**
	 * 按技能规则或场地规则分页查询威力倍率。
	 */
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		skillRuleId: Long?,
		terrainRuleId: Long?,
	): Page<BattleSkillTerrainPowerModifierResponse> {
		validatePage(page, size)
		return sqlClient.createQuery(BattleSkillTerrainPowerModifier::class) {
			skillRuleId?.let { where(table.skillRuleId eq requiredPositiveId(it, "skillRuleId")) }
			terrainRuleId?.let { where(table.terrainRuleId eq requiredPositiveId(it, "terrainRuleId")) }
			orderBy(table.skillRuleId, table.sortOrder, table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	/**
	 * 读取单条技能场地威力倍率。
	 */
	@Transactional(readOnly = true)
	fun get(id: Long): BattleSkillTerrainPowerModifierResponse =
		modifierByIdOrNotFound(id).toResponse()

	/**
	 * 创建技能场地威力倍率。
	 */
	@Transactional
	fun create(request: BattleSkillTerrainPowerModifierRequest): BattleSkillTerrainPowerModifierResponse {
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.terrainRuleId)
		ensureModifierAvailable(normalized, null)
		return repository.save(
			BattleSkillTerrainPowerModifier {
				skillRuleId = normalized.skillRuleId
				terrainRuleId = normalized.terrainRuleId
				powerMultiplier = normalized.powerMultiplier
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 更新技能场地威力倍率。
	 */
	@Transactional
	fun update(id: Long, request: BattleSkillTerrainPowerModifierRequest): BattleSkillTerrainPowerModifierResponse {
		modifierByIdOrNotFound(id)
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.terrainRuleId)
		ensureModifierAvailable(normalized, id)
		return repository.save(
			BattleSkillTerrainPowerModifier {
				this.id = id
				skillRuleId = normalized.skillRuleId
				terrainRuleId = normalized.terrainRuleId
				powerMultiplier = normalized.powerMultiplier
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 删除技能场地威力倍率。
	 */
	@Transactional
	fun delete(id: Long) {
		modifierByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun modifierByIdOrNotFound(id: Long): BattleSkillTerrainPowerModifier =
		repository.findNullable(id) ?: notFound("id", "技能场地威力倍率不存在: $id")

	private fun validateReferences(skillRuleId: Long, terrainRuleId: Long) {
		if (skillRuleRepository.findNullable(skillRuleId) == null) {
			invalidReference("skillRuleId", "技能规则不存在: $skillRuleId")
		}
		val terrainRule = terrainRuleRepository.findNullable(terrainRuleId)
			?: invalidReference("terrainRuleId", "场地规则不存在: $terrainRuleId")
		if (terrainRule.code !in SUPPORTED_TERRAIN_CODES) {
			invalidValue("terrainRuleId", "场地威力倍率只支持现代主系列四种场地: ${terrainRule.code}")
		}
	}

	private fun ensureModifierAvailable(request: BattleSkillTerrainPowerModifierRequest, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleSkillTerrainPowerModifier::class) {
			where(table.skillRuleId eq request.skillRuleId)
			where(table.terrainRuleId eq request.terrainRuleId)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("terrainRuleId", "该技能规则已经配置相同场地威力倍率")
		}
	}

	private fun BattleSkillTerrainPowerModifierRequest.normalized(): BattleSkillTerrainPowerModifierRequest {
		if (!powerMultiplier.isFinite() || powerMultiplier <= 0.0 || powerMultiplier > 10.0) {
			invalidValue("powerMultiplier", "powerMultiplier 必须大于 0 且不超过 10")
		}
		return copy(
			skillRuleId = requiredPositiveId(skillRuleId, "skillRuleId"),
			terrainRuleId = requiredPositiveId(terrainRuleId, "terrainRuleId"),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)
	}

	private fun BattleSkillTerrainPowerModifier.toResponse(): BattleSkillTerrainPowerModifierResponse =
		BattleSkillTerrainPowerModifierResponse(
			id = id,
			skillRuleId = skillRuleId,
			terrainRuleId = terrainRuleId,
			powerMultiplier = powerMultiplier,
			enabled = enabled,
			sortOrder = sortOrder,
		)

	private companion object {
		private val SUPPORTED_TERRAIN_CODES = setOf(
			"electric-terrain",
			"grassy-terrain",
			"misty-terrain",
			"psychic-terrain",
		)
	}
}
