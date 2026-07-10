package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleSkillGlobalFieldEffectRequest
import io.github.lishangbu.battlerules.dto.BattleSkillGlobalFieldEffectResponse
import io.github.lishangbu.battlerules.entity.BattleSkillGlobalFieldEffect
import io.github.lishangbu.battlerules.entity.chancePercent
import io.github.lishangbu.battlerules.entity.effectScope
import io.github.lishangbu.battlerules.entity.effectTiming
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.fieldRuleId
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.requiredWeatherRuleId
import io.github.lishangbu.battlerules.entity.skillRuleId
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.repository.BattleFieldRuleRepository
import io.github.lishangbu.battlerules.repository.BattleSkillGlobalFieldEffectRepository
import io.github.lishangbu.battlerules.repository.BattleSkillRuleRepository
import io.github.lishangbu.battlerules.repository.BattleWeatherRuleRepository
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
 * 技能全场效果维护服务。
 *
 * 该服务维护“技能规则命中后建立哪个全场效果”的结构化关系。它不会解释场上效果策略文本；
 * 解释工作集中在 `BattleRuntimeSnapshotService`，因此 CRUD 层只负责资料完整性、外键和唯一性。
 *
 * 本服务只允许引用 FIELD 作用域的 `battle_field_rule`。如果效果需要 USER_SIDE 或 TARGET_SIDE，
 * 应维护在 `BattleSkillFieldEffectService` 中；这样两类效果不会共享含义不同的字段。
 */
@Service
class BattleSkillGlobalFieldEffectService(
	private val repository: BattleSkillGlobalFieldEffectRepository,
	private val skillRuleRepository: BattleSkillRuleRepository,
	private val fieldRuleRepository: BattleFieldRuleRepository,
	private val weatherRuleRepository: BattleWeatherRuleRepository,
	private val sqlClient: KSqlClient,
) {
	/**
	 * 按技能规则或全场效果分页查询技能全场效果。
	 */
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, skillRuleId: Long?, fieldRuleId: Long?): Page<BattleSkillGlobalFieldEffectResponse> {
		validatePage(page, size)
		return sqlClient.createQuery(BattleSkillGlobalFieldEffect::class) {
			skillRuleId?.let { where(table.skillRuleId eq requiredPositiveId(it, "skillRuleId")) }
			fieldRuleId?.let { where(table.fieldRuleId eq requiredPositiveId(it, "fieldRuleId")) }
			orderBy(table.skillRuleId, table.sortOrder, table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	/**
	 * 读取单条技能全场效果。
	 */
	@Transactional(readOnly = true)
	fun get(id: Long): BattleSkillGlobalFieldEffectResponse =
		effectByIdOrNotFound(id).toResponse()

	/**
	 * 创建技能全场效果。
	 */
	@Transactional
	fun create(request: BattleSkillGlobalFieldEffectRequest): BattleSkillGlobalFieldEffectResponse {
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.fieldRuleId)
		ensureEffectAvailable(normalized, null)
		return repository.save(
			BattleSkillGlobalFieldEffect {
				skillRuleId = normalized.skillRuleId
				fieldRuleId = normalized.fieldRuleId
				effectTiming = normalized.effectTiming
				requiredWeatherRuleId = normalized.requiredWeatherRuleId
				chancePercent = normalized.chancePercent
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 更新技能全场效果。
	 */
	@Transactional
	fun update(id: Long, request: BattleSkillGlobalFieldEffectRequest): BattleSkillGlobalFieldEffectResponse {
		effectByIdOrNotFound(id)
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.fieldRuleId)
		ensureEffectAvailable(normalized, id)
		return repository.save(
			BattleSkillGlobalFieldEffect {
				this.id = id
				skillRuleId = normalized.skillRuleId
				fieldRuleId = normalized.fieldRuleId
				effectTiming = normalized.effectTiming
				requiredWeatherRuleId = normalized.requiredWeatherRuleId
				chancePercent = normalized.chancePercent
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 删除技能全场效果。
	 */
	@Transactional
	fun delete(id: Long) {
		effectByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun effectByIdOrNotFound(id: Long): BattleSkillGlobalFieldEffect =
		repository.findNullable(id) ?: notFound("id", "技能全场效果不存在: $id")

	private fun validateReferences(skillRuleId: Long, fieldRuleId: Long) {
		if (skillRuleRepository.findNullable(skillRuleId) == null) {
			invalidReference("skillRuleId", "技能规则不存在: $skillRuleId")
		}
		val fieldRule = fieldRuleRepository.findNullable(fieldRuleId)
			?: invalidReference("fieldRuleId", "场上效果规则不存在: $fieldRuleId")
		if (fieldRule.effectScope != "FIELD") {
			invalidValue("fieldRuleId", "技能全场效果只能引用 FIELD 作用域的场上效果")
		}
	}

	private fun validateRequiredWeather(requiredWeatherRuleId: Long?) {
		if (requiredWeatherRuleId == null) {
			return
		}
		val weatherRule = weatherRuleRepository.findNullable(requiredWeatherRuleId)
			?: invalidReference("requiredWeatherRuleId", "天气规则不存在: $requiredWeatherRuleId")
		if (weatherRule.code == "clear") {
			invalidValue("requiredWeatherRuleId", "天气前置条件不能引用无天气")
		}
	}

	private fun ensureEffectAvailable(request: BattleSkillGlobalFieldEffectRequest, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleSkillGlobalFieldEffect::class) {
			where(table.skillRuleId eq request.skillRuleId)
			where(table.fieldRuleId eq request.fieldRuleId)
			where(table.effectTiming eq request.effectTiming)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("fieldRuleId", "该技能规则已经配置相同全场效果")
		}
	}

	private fun BattleSkillGlobalFieldEffectRequest.normalized(): BattleSkillGlobalFieldEffectRequest =
		copy(
			skillRuleId = requiredPositiveId(skillRuleId, "skillRuleId"),
			fieldRuleId = requiredPositiveId(fieldRuleId, "fieldRuleId"),
			effectTiming = requiredEnumText(effectTiming, "effectTiming", setOf("AFTER_HIT")),
			requiredWeatherRuleId = requiredWeatherRuleId?.let { requiredPositiveId(it, "requiredWeatherRuleId") },
			chancePercent = requiredIntRange(chancePercent, "chancePercent", 1, 100),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		).also {
			validateRequiredWeather(it.requiredWeatherRuleId)
		}

	private fun BattleSkillGlobalFieldEffect.toResponse(): BattleSkillGlobalFieldEffectResponse =
		BattleSkillGlobalFieldEffectResponse {
			id = this@toResponse.id
			skillRuleId = this@toResponse.skillRuleId
			fieldRuleId = this@toResponse.fieldRuleId
			effectTiming = this@toResponse.effectTiming
			requiredWeatherRuleId = this@toResponse.requiredWeatherRuleId
			chancePercent = this@toResponse.chancePercent
			enabled = this@toResponse.enabled
			sortOrder = this@toResponse.sortOrder
		}
}
