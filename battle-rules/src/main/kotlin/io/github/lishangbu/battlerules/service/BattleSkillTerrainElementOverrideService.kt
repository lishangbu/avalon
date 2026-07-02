package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleSkillTerrainElementOverrideRequest
import io.github.lishangbu.battlerules.dto.BattleSkillTerrainElementOverrideResponse
import io.github.lishangbu.battlerules.entity.BattleSkillTerrainElementOverride
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.skillRuleId
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.targetElementId
import io.github.lishangbu.battlerules.entity.terrainRuleId
import io.github.lishangbu.battlerules.repository.BattleSkillRuleRepository
import io.github.lishangbu.battlerules.repository.BattleSkillTerrainElementOverrideRepository
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 技能场地属性覆盖维护服务。
 *
 * 该服务维护“技能规则 + 场地规则 => 目标属性”的数据关系，用于场地脉冲一类按当前场地改变结算属性的技能。
 * 属性覆盖独立于威力倍率，因为它不仅影响普通伤害，还会影响命中前属性吸收、属性一致加成、属性道具、火属性解冻
 * 等所有读取技能有效属性的规则。
 *
 * 同一技能规则在同一场地下只能有一条属性覆盖；这保证运行时可以装配成确定的 Map。场地规则同样限制为引擎支持的
 * 现代主系列四场地，避免维护端先写入引擎无法识别的 code。
 */
@Service
class BattleSkillTerrainElementOverrideService(
	private val repository: BattleSkillTerrainElementOverrideRepository,
	private val skillRuleRepository: BattleSkillRuleRepository,
	private val terrainRuleRepository: BattleTerrainRuleRepository,
	private val sqlClient: KSqlClient,
	private val jdbcTemplate: JdbcTemplate,
) {
	/**
	 * 按技能规则、场地规则或目标属性分页查询属性覆盖。
	 */
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		skillRuleId: Long?,
		terrainRuleId: Long?,
		targetElementId: Long?,
	): Page<BattleSkillTerrainElementOverrideResponse> {
		validatePage(page, size)
		return sqlClient.createQuery(BattleSkillTerrainElementOverride::class) {
			skillRuleId?.let { where(table.skillRuleId eq requiredPositiveId(it, "skillRuleId")) }
			terrainRuleId?.let { where(table.terrainRuleId eq requiredPositiveId(it, "terrainRuleId")) }
			targetElementId?.let { where(table.targetElementId eq requiredPositiveId(it, "targetElementId")) }
			orderBy(table.skillRuleId, table.sortOrder, table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	/**
	 * 读取单条技能场地属性覆盖。
	 */
	@Transactional(readOnly = true)
	fun get(id: Long): BattleSkillTerrainElementOverrideResponse =
		overrideByIdOrNotFound(id).toResponse()

	/**
	 * 创建技能场地属性覆盖。
	 */
	@Transactional
	fun create(request: BattleSkillTerrainElementOverrideRequest): BattleSkillTerrainElementOverrideResponse {
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.terrainRuleId, normalized.targetElementId)
		ensureOverrideAvailable(normalized, null)
		return repository.save(
			BattleSkillTerrainElementOverride {
				skillRuleId = normalized.skillRuleId
				terrainRuleId = normalized.terrainRuleId
				targetElementId = normalized.targetElementId
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 更新技能场地属性覆盖。
	 */
	@Transactional
	fun update(id: Long, request: BattleSkillTerrainElementOverrideRequest): BattleSkillTerrainElementOverrideResponse {
		overrideByIdOrNotFound(id)
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.terrainRuleId, normalized.targetElementId)
		ensureOverrideAvailable(normalized, id)
		return repository.save(
			BattleSkillTerrainElementOverride {
				this.id = id
				skillRuleId = normalized.skillRuleId
				terrainRuleId = normalized.terrainRuleId
				targetElementId = normalized.targetElementId
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 删除技能场地属性覆盖。
	 */
	@Transactional
	fun delete(id: Long) {
		overrideByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun overrideByIdOrNotFound(id: Long): BattleSkillTerrainElementOverride =
		repository.findNullable(id) ?: notFound("id", "技能场地属性覆盖不存在: $id")

	private fun validateReferences(skillRuleId: Long, terrainRuleId: Long, targetElementId: Long) {
		if (skillRuleRepository.findNullable(skillRuleId) == null) {
			invalidReference("skillRuleId", "技能规则不存在: $skillRuleId")
		}
		val terrainRule = terrainRuleRepository.findNullable(terrainRuleId)
			?: invalidReference("terrainRuleId", "场地规则不存在: $terrainRuleId")
		if (terrainRule.code !in SUPPORTED_TERRAIN_CODES) {
			invalidValue("terrainRuleId", "场地属性覆盖只支持现代主系列四种场地: ${terrainRule.code}")
		}
		val elementExists = jdbcTemplate.queryForObject(
			"select exists(select 1 from game_element where id = ? and enabled = true)",
			Boolean::class.java,
			targetElementId,
		) == true
		if (!elementExists) {
			invalidReference("targetElementId", "目标属性不存在: $targetElementId")
		}
	}

	private fun ensureOverrideAvailable(request: BattleSkillTerrainElementOverrideRequest, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleSkillTerrainElementOverride::class) {
			where(table.skillRuleId eq request.skillRuleId)
			where(table.terrainRuleId eq request.terrainRuleId)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("terrainRuleId", "该技能规则已经配置相同场地属性覆盖")
		}
	}

	private fun BattleSkillTerrainElementOverrideRequest.normalized(): BattleSkillTerrainElementOverrideRequest =
		copy(
			skillRuleId = requiredPositiveId(skillRuleId, "skillRuleId"),
			terrainRuleId = requiredPositiveId(terrainRuleId, "terrainRuleId"),
			targetElementId = requiredPositiveId(targetElementId, "targetElementId"),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleSkillTerrainElementOverride.toResponse(): BattleSkillTerrainElementOverrideResponse =
		BattleSkillTerrainElementOverrideResponse(
			id = id,
			skillRuleId = skillRuleId,
			terrainRuleId = terrainRuleId,
			targetElementId = targetElementId,
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
