package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleSkillWeatherPowerModifierRequest
import io.github.lishangbu.battlerules.dto.BattleSkillWeatherPowerModifierResponse
import io.github.lishangbu.battlerules.entity.BattleSkillWeatherPowerModifier
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.powerMultiplier
import io.github.lishangbu.battlerules.entity.skillRuleId
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.weatherRuleId
import io.github.lishangbu.battlerules.repository.BattleSkillRuleRepository
import io.github.lishangbu.battlerules.repository.BattleSkillWeatherPowerModifierRepository
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
 * 技能天气威力倍率维护服务。
 *
 * 有些技能的威力会在指定天气下被放大或缩小，例如日光束在雨、沙暴、雪中威力减半，气象球在真实天气下威力翻倍。
 * 本服务只维护“技能规则 + 天气规则 => 威力倍率”的数据关系，不处理技能属性变化、蓄力回合或天气持续时间。
 *
 * 倍率必须是有限正数，并限制在一个保守上限内，防止维护端录入 NaN、无穷大或明显不可能的倍率。
 * 与命中覆盖一样，这里禁止引用“无天气”，因为无天气应表现为没有倍率条目，而不是一条倍率为 1 的规则。
 */
@Service
class BattleSkillWeatherPowerModifierService(
	private val repository: BattleSkillWeatherPowerModifierRepository,
	private val skillRuleRepository: BattleSkillRuleRepository,
	private val weatherRuleRepository: BattleWeatherRuleRepository,
	private val sqlClient: KSqlClient,
) {
	/**
	 * 按技能规则或天气规则分页查询威力倍率。
	 */
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		skillRuleId: Long?,
		weatherRuleId: Long?,
	): Page<BattleSkillWeatherPowerModifierResponse> {
		validatePage(page, size)
		return sqlClient.createQuery(BattleSkillWeatherPowerModifier::class) {
			skillRuleId?.let { where(table.skillRuleId eq requiredPositiveId(it, "skillRuleId")) }
			weatherRuleId?.let { where(table.weatherRuleId eq requiredPositiveId(it, "weatherRuleId")) }
			orderBy(table.skillRuleId, table.sortOrder, table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	/**
	 * 读取单条技能天气威力倍率。
	 */
	@Transactional(readOnly = true)
	fun get(id: Long): BattleSkillWeatherPowerModifierResponse =
		modifierByIdOrNotFound(id).toResponse()

	/**
	 * 创建技能天气威力倍率。
	 */
	@Transactional
	fun create(request: BattleSkillWeatherPowerModifierRequest): BattleSkillWeatherPowerModifierResponse {
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.weatherRuleId)
		ensureModifierAvailable(normalized, null)
		return repository.save(
			BattleSkillWeatherPowerModifier {
				skillRuleId = normalized.skillRuleId
				weatherRuleId = normalized.weatherRuleId
				powerMultiplier = normalized.powerMultiplier
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 更新技能天气威力倍率。
	 */
	@Transactional
	fun update(id: Long, request: BattleSkillWeatherPowerModifierRequest): BattleSkillWeatherPowerModifierResponse {
		modifierByIdOrNotFound(id)
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.weatherRuleId)
		ensureModifierAvailable(normalized, id)
		return repository.save(
			BattleSkillWeatherPowerModifier {
				this.id = id
				skillRuleId = normalized.skillRuleId
				weatherRuleId = normalized.weatherRuleId
				powerMultiplier = normalized.powerMultiplier
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 删除技能天气威力倍率。
	 */
	@Transactional
	fun delete(id: Long) {
		modifierByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun modifierByIdOrNotFound(id: Long): BattleSkillWeatherPowerModifier =
		repository.findNullable(id) ?: notFound("id", "技能天气威力倍率不存在: $id")

	private fun validateReferences(skillRuleId: Long, weatherRuleId: Long) {
		if (skillRuleRepository.findNullable(skillRuleId) == null) {
			invalidReference("skillRuleId", "技能规则不存在: $skillRuleId")
		}
		val weatherRule = weatherRuleRepository.findNullable(weatherRuleId)
			?: invalidReference("weatherRuleId", "天气规则不存在: $weatherRuleId")
		if (weatherRule.code == "clear") {
			invalidValue("weatherRuleId", "天气威力倍率不能引用无天气")
		}
	}

	private fun ensureModifierAvailable(request: BattleSkillWeatherPowerModifierRequest, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleSkillWeatherPowerModifier::class) {
			where(table.skillRuleId eq request.skillRuleId)
			where(table.weatherRuleId eq request.weatherRuleId)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("weatherRuleId", "该技能规则已经配置相同天气威力倍率")
		}
	}

	private fun BattleSkillWeatherPowerModifierRequest.normalized(): BattleSkillWeatherPowerModifierRequest {
		if (!powerMultiplier.isFinite() || powerMultiplier <= 0.0 || powerMultiplier > 10.0) {
			invalidValue("powerMultiplier", "powerMultiplier 必须大于 0 且不超过 10")
		}
		return copy(
			skillRuleId = requiredPositiveId(skillRuleId, "skillRuleId"),
			weatherRuleId = requiredPositiveId(weatherRuleId, "weatherRuleId"),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)
	}

	private fun BattleSkillWeatherPowerModifier.toResponse(): BattleSkillWeatherPowerModifierResponse =
		BattleSkillWeatherPowerModifierResponse(
			id = id,
			skillRuleId = skillRuleId,
			weatherRuleId = weatherRuleId,
			powerMultiplier = powerMultiplier,
			enabled = enabled,
			sortOrder = sortOrder,
		)
}
