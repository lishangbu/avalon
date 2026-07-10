package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleSkillWeatherElementOverrideRequest
import io.github.lishangbu.battlerules.dto.BattleSkillWeatherElementOverrideResponse
import io.github.lishangbu.battlerules.entity.BattleSkillWeatherElementOverride
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.skillRuleId
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.targetElementId
import io.github.lishangbu.battlerules.entity.weatherRuleId
import io.github.lishangbu.battlerules.repository.BattleSkillRuleRepository
import io.github.lishangbu.battlerules.repository.BattleSkillWeatherElementOverrideRepository
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
 * 技能天气属性覆盖维护服务。
 *
 * 该服务维护“技能规则 + 天气规则 => 目标属性”的结构化资料，用于表达气象球一类技能在天气下改变本次
 * 结算属性的规则。属性覆盖会影响伤害公式和命中后的规则钩子，因此它独立于天气威力倍率维护，避免把
 * “威力翻倍”和“变为水属性”等不同事实塞进同一字段。
 *
 * 服务层会校验技能规则、天气规则和目标属性均存在，并禁止引用“无天气”。同一技能规则在同一天气下只能有
 * 一条属性覆盖，运行时据此可以稳定装配为 Map，而不需要处理冲突优先级。
 */
@Service
class BattleSkillWeatherElementOverrideService(
	private val repository: BattleSkillWeatherElementOverrideRepository,
	private val skillRuleRepository: BattleSkillRuleRepository,
	private val weatherRuleRepository: BattleWeatherRuleRepository,
	private val sqlClient: KSqlClient,
) {
	/**
	 * 按技能规则、天气规则或目标属性分页查询属性覆盖。
	 */
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		skillRuleId: Long?,
		weatherRuleId: Long?,
		targetElementId: Long?,
	): Page<BattleSkillWeatherElementOverrideResponse> {
		validatePage(page, size)
		return sqlClient.createQuery(BattleSkillWeatherElementOverride::class) {
			skillRuleId?.let { where(table.skillRuleId eq requiredPositiveId(it, "skillRuleId")) }
			weatherRuleId?.let { where(table.weatherRuleId eq requiredPositiveId(it, "weatherRuleId")) }
			targetElementId?.let { where(table.targetElementId eq requiredPositiveId(it, "targetElementId")) }
			orderBy(table.skillRuleId, table.sortOrder, table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	/**
	 * 读取单条技能天气属性覆盖。
	 */
	@Transactional(readOnly = true)
	fun get(id: Long): BattleSkillWeatherElementOverrideResponse =
		overrideByIdOrNotFound(id).toResponse()

	/**
	 * 创建技能天气属性覆盖。
	 */
	@Transactional
	fun create(request: BattleSkillWeatherElementOverrideRequest): BattleSkillWeatherElementOverrideResponse {
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.weatherRuleId, normalized.targetElementId)
		ensureOverrideAvailable(normalized, null)
		return repository.save(
			BattleSkillWeatherElementOverride {
				skillRuleId = normalized.skillRuleId
				weatherRuleId = normalized.weatherRuleId
				targetElementId = normalized.targetElementId
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 更新技能天气属性覆盖。
	 */
	@Transactional
	fun update(id: Long, request: BattleSkillWeatherElementOverrideRequest): BattleSkillWeatherElementOverrideResponse {
		overrideByIdOrNotFound(id)
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.weatherRuleId, normalized.targetElementId)
		ensureOverrideAvailable(normalized, id)
		return repository.save(
			BattleSkillWeatherElementOverride {
				this.id = id
				skillRuleId = normalized.skillRuleId
				weatherRuleId = normalized.weatherRuleId
				targetElementId = normalized.targetElementId
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 删除技能天气属性覆盖。
	 */
	@Transactional
	fun delete(id: Long) {
		overrideByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun overrideByIdOrNotFound(id: Long): BattleSkillWeatherElementOverride =
		repository.findNullable(id) ?: notFound("id", "技能天气属性覆盖不存在: $id")

	private fun validateReferences(skillRuleId: Long, weatherRuleId: Long, targetElementId: Long) {
		if (skillRuleRepository.findNullable(skillRuleId) == null) {
			invalidReference("skillRuleId", "技能规则不存在: $skillRuleId")
		}
		val weatherRule = weatherRuleRepository.findNullable(weatherRuleId)
			?: invalidReference("weatherRuleId", "天气规则不存在: $weatherRuleId")
		if (weatherRule.code == "clear") {
			invalidValue("weatherRuleId", "天气属性覆盖不能引用无天气")
		}
		requireEnabledGameElementReference(sqlClient, targetElementId, "targetElementId", "目标属性")
	}

	private fun ensureOverrideAvailable(request: BattleSkillWeatherElementOverrideRequest, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleSkillWeatherElementOverride::class) {
			where(table.skillRuleId eq request.skillRuleId)
			where(table.weatherRuleId eq request.weatherRuleId)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("weatherRuleId", "该技能规则已经配置相同天气属性覆盖")
		}
	}

	private fun BattleSkillWeatherElementOverrideRequest.normalized(): BattleSkillWeatherElementOverrideRequest =
		copy(
			skillRuleId = requiredPositiveId(skillRuleId, "skillRuleId"),
			weatherRuleId = requiredPositiveId(weatherRuleId, "weatherRuleId"),
			targetElementId = requiredPositiveId(targetElementId, "targetElementId"),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleSkillWeatherElementOverride.toResponse(): BattleSkillWeatherElementOverrideResponse =
		BattleSkillWeatherElementOverrideResponse {
			id = this@toResponse.id
			skillRuleId = this@toResponse.skillRuleId
			weatherRuleId = this@toResponse.weatherRuleId
			targetElementId = this@toResponse.targetElementId
			enabled = this@toResponse.enabled
			sortOrder = this@toResponse.sortOrder
		}
}
