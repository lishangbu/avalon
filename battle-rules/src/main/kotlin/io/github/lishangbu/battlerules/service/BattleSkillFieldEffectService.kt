package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleSkillFieldEffectRequest
import io.github.lishangbu.battlerules.dto.BattleSkillFieldEffectResponse
import io.github.lishangbu.battlerules.entity.BattleSkillFieldEffect
import io.github.lishangbu.battlerules.entity.chancePercent
import io.github.lishangbu.battlerules.entity.effectScope
import io.github.lishangbu.battlerules.entity.effectTiming
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.fieldRuleId
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.requiredWeatherRuleId
import io.github.lishangbu.battlerules.entity.skillRuleId
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.targetSide
import io.github.lishangbu.battlerules.repository.BattleFieldRuleRepository
import io.github.lishangbu.battlerules.repository.BattleSkillFieldEffectRepository
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
 * 技能场上效果维护服务。
 *
 * 该服务维护“技能规则命中后建立哪个场上效果”的结构化关系。它不会把场上效果策略文本解释成引擎行为；
 * 解释工作集中在 `BattleRuntimeSnapshotService`，这样 CRUD 层保持纯资料维护，运行时适配层负责强类型映射。
 *
 * 当前只允许引用 SIDE 作用域的场上效果，因为本表的 `targetSide` 明确表达使用者侧或目标侧。全场空间类效果
 * 以后应使用独立字段或独立资源建模，避免把不同作用域的规则塞进同一套语义里。
 */
@Service
class BattleSkillFieldEffectService(
	private val repository: BattleSkillFieldEffectRepository,
	private val skillRuleRepository: BattleSkillRuleRepository,
	private val fieldRuleRepository: BattleFieldRuleRepository,
	private val weatherRuleRepository: BattleWeatherRuleRepository,
	private val sqlClient: KSqlClient,
) {
	/**
	 * 按技能规则或场上效果分页查询技能场上效果。
	 */
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, skillRuleId: Long?, fieldRuleId: Long?): Page<BattleSkillFieldEffectResponse> {
		validatePage(page, size)
		return sqlClient.createQuery(BattleSkillFieldEffect::class) {
			skillRuleId?.let { where(table.skillRuleId eq requiredPositiveId(it, "skillRuleId")) }
			fieldRuleId?.let { where(table.fieldRuleId eq requiredPositiveId(it, "fieldRuleId")) }
			orderBy(table.skillRuleId, table.sortOrder, table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	/**
	 * 读取单条技能场上效果。
	 */
	@Transactional(readOnly = true)
	fun get(id: Long): BattleSkillFieldEffectResponse =
		effectByIdOrNotFound(id).toResponse()

	/**
	 * 创建技能场上效果。
	 */
	@Transactional
	fun create(request: BattleSkillFieldEffectRequest): BattleSkillFieldEffectResponse {
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.fieldRuleId)
		ensureEffectAvailable(normalized, null)
		return repository.save(
			BattleSkillFieldEffect {
				skillRuleId = normalized.skillRuleId
				fieldRuleId = normalized.fieldRuleId
				targetSide = normalized.targetSide
				effectTiming = normalized.effectTiming
				requiredWeatherRuleId = normalized.requiredWeatherRuleId
				chancePercent = normalized.chancePercent
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 更新技能场上效果。
	 */
	@Transactional
	fun update(id: Long, request: BattleSkillFieldEffectRequest): BattleSkillFieldEffectResponse {
		effectByIdOrNotFound(id)
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.fieldRuleId)
		ensureEffectAvailable(normalized, id)
		return repository.save(
			BattleSkillFieldEffect {
				this.id = id
				skillRuleId = normalized.skillRuleId
				fieldRuleId = normalized.fieldRuleId
				targetSide = normalized.targetSide
				effectTiming = normalized.effectTiming
				requiredWeatherRuleId = normalized.requiredWeatherRuleId
				chancePercent = normalized.chancePercent
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 删除技能场上效果。
	 */
	@Transactional
	fun delete(id: Long) {
		effectByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun effectByIdOrNotFound(id: Long): BattleSkillFieldEffect =
		repository.findNullable(id) ?: notFound("id", "技能场上效果不存在: $id")

	private fun validateReferences(skillRuleId: Long, fieldRuleId: Long) {
		if (skillRuleRepository.findNullable(skillRuleId) == null) {
			invalidReference("skillRuleId", "技能规则不存在: $skillRuleId")
		}
		val fieldRule = fieldRuleRepository.findNullable(fieldRuleId)
			?: invalidReference("fieldRuleId", "场上效果规则不存在: $fieldRuleId")
		if (fieldRule.effectScope != "SIDE") {
			invalidValue("fieldRuleId", "技能场上效果只能引用 SIDE 作用域的场上效果")
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

	private fun ensureEffectAvailable(request: BattleSkillFieldEffectRequest, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleSkillFieldEffect::class) {
			where(table.skillRuleId eq request.skillRuleId)
			where(table.fieldRuleId eq request.fieldRuleId)
			where(table.targetSide eq request.targetSide)
			where(table.effectTiming eq request.effectTiming)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("fieldRuleId", "该技能规则已经配置相同场上效果")
		}
	}

	private fun BattleSkillFieldEffectRequest.normalized(): BattleSkillFieldEffectRequest =
		copy(
			skillRuleId = requiredPositiveId(skillRuleId, "skillRuleId"),
			fieldRuleId = requiredPositiveId(fieldRuleId, "fieldRuleId"),
			targetSide = requiredEnumText(targetSide, "targetSide", setOf("USER_SIDE", "TARGET_SIDE")),
			effectTiming = requiredEnumText(effectTiming, "effectTiming", setOf("AFTER_HIT")),
			requiredWeatherRuleId = requiredWeatherRuleId?.let { requiredPositiveId(it, "requiredWeatherRuleId") },
			chancePercent = requiredIntRange(chancePercent, "chancePercent", 1, 100),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		).also {
			validateRequiredWeather(it.requiredWeatherRuleId)
		}

	private fun BattleSkillFieldEffect.toResponse(): BattleSkillFieldEffectResponse =
		BattleSkillFieldEffectResponse(
			id = id,
			skillRuleId = skillRuleId,
			fieldRuleId = fieldRuleId,
			targetSide = targetSide,
			effectTiming = effectTiming,
			requiredWeatherRuleId = requiredWeatherRuleId,
			chancePercent = chancePercent,
			enabled = enabled,
			sortOrder = sortOrder,
		)
}
