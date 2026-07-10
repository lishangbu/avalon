package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleSkillWeatherAccuracyOverrideRequest
import io.github.lishangbu.battlerules.dto.BattleSkillWeatherAccuracyOverrideResponse
import io.github.lishangbu.battlerules.entity.BattleSkillWeatherAccuracyOverride
import io.github.lishangbu.battlerules.entity.accuracyPercent
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.skillRuleId
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.weatherRuleId
import io.github.lishangbu.battlerules.repository.BattleSkillRuleRepository
import io.github.lishangbu.battlerules.repository.BattleSkillWeatherAccuracyOverrideRepository
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
 * 技能天气命中覆盖维护服务。
 *
 * 现代战斗中存在“指定天气下必中”或“指定天气下降低命中”的技能，例如暴风雪、打雷和暴风。
 * 这些规则是技能规则的多值从属资料：一个技能规则可以针对多个天气分别配置命中覆盖，但同一技能规则
 * 在同一天气下只能有一条启用候选资料。
 *
 * 这里明确禁止引用“无天气”规则，因为引擎中的天气命中映射只接收真实天气键。`accuracyPercent = null`
 * 表示必中，非空值必须在 1 到 100 之间。这样的建模可以把“必中”和“100% 命中”区分开，便于以后继续处理
 * 闪避、命中阶级和必中判定之间的优先级。
 */
@Service
class BattleSkillWeatherAccuracyOverrideService(
	private val repository: BattleSkillWeatherAccuracyOverrideRepository,
	private val skillRuleRepository: BattleSkillRuleRepository,
	private val weatherRuleRepository: BattleWeatherRuleRepository,
	private val sqlClient: KSqlClient,
) {
	/**
	 * 按技能规则或天气规则分页查询命中覆盖。
	 */
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		skillRuleId: Long?,
		weatherRuleId: Long?,
	): Page<BattleSkillWeatherAccuracyOverrideResponse> {
		validatePage(page, size)
		return sqlClient.createQuery(BattleSkillWeatherAccuracyOverride::class) {
			skillRuleId?.let { where(table.skillRuleId eq requiredPositiveId(it, "skillRuleId")) }
			weatherRuleId?.let { where(table.weatherRuleId eq requiredPositiveId(it, "weatherRuleId")) }
			orderBy(table.skillRuleId, table.sortOrder, table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	/**
	 * 读取单条技能天气命中覆盖。
	 */
	@Transactional(readOnly = true)
	fun get(id: Long): BattleSkillWeatherAccuracyOverrideResponse =
		overrideByIdOrNotFound(id).toResponse()

	/**
	 * 创建技能天气命中覆盖。
	 */
	@Transactional
	fun create(request: BattleSkillWeatherAccuracyOverrideRequest): BattleSkillWeatherAccuracyOverrideResponse {
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.weatherRuleId)
		ensureOverrideAvailable(normalized, null)
		return repository.save(
			BattleSkillWeatherAccuracyOverride {
				skillRuleId = normalized.skillRuleId
				weatherRuleId = normalized.weatherRuleId
				accuracyPercent = normalized.accuracyPercent
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 更新技能天气命中覆盖。
	 */
	@Transactional
	fun update(
		id: Long,
		request: BattleSkillWeatherAccuracyOverrideRequest,
	): BattleSkillWeatherAccuracyOverrideResponse {
		overrideByIdOrNotFound(id)
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.weatherRuleId)
		ensureOverrideAvailable(normalized, id)
		return repository.save(
			BattleSkillWeatherAccuracyOverride {
				this.id = id
				skillRuleId = normalized.skillRuleId
				weatherRuleId = normalized.weatherRuleId
				accuracyPercent = normalized.accuracyPercent
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 删除技能天气命中覆盖。
	 */
	@Transactional
	fun delete(id: Long) {
		overrideByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun overrideByIdOrNotFound(id: Long): BattleSkillWeatherAccuracyOverride =
		repository.findNullable(id) ?: notFound("id", "技能天气命中覆盖不存在: $id")

	private fun validateReferences(skillRuleId: Long, weatherRuleId: Long) {
		if (skillRuleRepository.findNullable(skillRuleId) == null) {
			invalidReference("skillRuleId", "技能规则不存在: $skillRuleId")
		}
		val weatherRule = weatherRuleRepository.findNullable(weatherRuleId)
			?: invalidReference("weatherRuleId", "天气规则不存在: $weatherRuleId")
		if (weatherRule.code == "clear") {
			invalidValue("weatherRuleId", "天气命中覆盖不能引用无天气")
		}
	}

	private fun ensureOverrideAvailable(request: BattleSkillWeatherAccuracyOverrideRequest, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleSkillWeatherAccuracyOverride::class) {
			where(table.skillRuleId eq request.skillRuleId)
			where(table.weatherRuleId eq request.weatherRuleId)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("weatherRuleId", "该技能规则已经配置相同天气命中覆盖")
		}
	}

	private fun BattleSkillWeatherAccuracyOverrideRequest.normalized(): BattleSkillWeatherAccuracyOverrideRequest =
		copy(
			skillRuleId = requiredPositiveId(skillRuleId, "skillRuleId"),
			weatherRuleId = requiredPositiveId(weatherRuleId, "weatherRuleId"),
			accuracyPercent = optionalIntRange(accuracyPercent, "accuracyPercent", 1, 100),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleSkillWeatherAccuracyOverride.toResponse(): BattleSkillWeatherAccuracyOverrideResponse =
		BattleSkillWeatherAccuracyOverrideResponse {
			id = this@toResponse.id
			skillRuleId = this@toResponse.skillRuleId
			weatherRuleId = this@toResponse.weatherRuleId
			accuracyPercent = this@toResponse.accuracyPercent
			enabled = this@toResponse.enabled
			sortOrder = this@toResponse.sortOrder
		}
}
