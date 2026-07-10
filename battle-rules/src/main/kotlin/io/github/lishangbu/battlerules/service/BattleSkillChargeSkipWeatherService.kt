package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleSkillChargeSkipWeatherRequest
import io.github.lishangbu.battlerules.dto.BattleSkillChargeSkipWeatherResponse
import io.github.lishangbu.battlerules.entity.BattleSkillChargeSkipWeather
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.skillRuleId
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.weatherRuleId
import io.github.lishangbu.battlerules.repository.BattleSkillChargeSkipWeatherRepository
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
 * 技能跳过蓄力天气维护服务。
 *
 * 这个服务维护“哪些天气能让某个蓄力技能跳过等待回合”。它不根据技能名称或技能 ID 写死特殊规则，而是要求
 * 维护端显式把技能主规则标记为 `chargesBeforeUse`，再为该技能选择可跳过蓄力的天气。
 *
 * 这样做能防止把晴天加速错误套到所有蓄力技能上；引擎运行时只读取这里形成的天气集合，保持状态机纯净且可测试。
 */
@Service
class BattleSkillChargeSkipWeatherService(
	private val repository: BattleSkillChargeSkipWeatherRepository,
	private val skillRuleRepository: BattleSkillRuleRepository,
	private val weatherRuleRepository: BattleWeatherRuleRepository,
	private val sqlClient: KSqlClient,
) {
	/**
	 * 按技能规则或天气规则分页查询跳过蓄力天气。
	 */
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		skillRuleId: Long?,
		weatherRuleId: Long?,
	): Page<BattleSkillChargeSkipWeatherResponse> {
		validatePage(page, size)
		return sqlClient.createQuery(BattleSkillChargeSkipWeather::class) {
			skillRuleId?.let { where(table.skillRuleId eq requiredPositiveId(it, "skillRuleId")) }
			weatherRuleId?.let { where(table.weatherRuleId eq requiredPositiveId(it, "weatherRuleId")) }
			orderBy(table.skillRuleId, table.sortOrder, table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	/**
	 * 读取单条技能跳过蓄力天气。
	 */
	@Transactional(readOnly = true)
	fun get(id: Long): BattleSkillChargeSkipWeatherResponse =
		ruleByIdOrNotFound(id).toResponse()

	/**
	 * 创建技能跳过蓄力天气。
	 */
	@Transactional
	fun create(request: BattleSkillChargeSkipWeatherRequest): BattleSkillChargeSkipWeatherResponse {
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.weatherRuleId)
		ensureRuleAvailable(normalized, null)
		return repository.save(
			BattleSkillChargeSkipWeather {
				skillRuleId = normalized.skillRuleId
				weatherRuleId = normalized.weatherRuleId
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 更新技能跳过蓄力天气。
	 */
	@Transactional
	fun update(id: Long, request: BattleSkillChargeSkipWeatherRequest): BattleSkillChargeSkipWeatherResponse {
		ruleByIdOrNotFound(id)
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.weatherRuleId)
		ensureRuleAvailable(normalized, id)
		return repository.save(
			BattleSkillChargeSkipWeather {
				this.id = id
				skillRuleId = normalized.skillRuleId
				weatherRuleId = normalized.weatherRuleId
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 删除技能跳过蓄力天气。
	 */
	@Transactional
	fun delete(id: Long) {
		ruleByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun ruleByIdOrNotFound(id: Long): BattleSkillChargeSkipWeather =
		repository.findNullable(id) ?: notFound("id", "技能跳过蓄力天气不存在: $id")

	private fun validateReferences(skillRuleId: Long, weatherRuleId: Long) {
		val skillRule = skillRuleRepository.findNullable(skillRuleId)
			?: invalidReference("skillRuleId", "技能规则不存在: $skillRuleId")
		if (!skillRule.chargesBeforeUse) {
			invalidValue("skillRuleId", "只有蓄力后发动的技能规则才能配置跳过蓄力天气")
		}
		val weatherRule = weatherRuleRepository.findNullable(weatherRuleId)
			?: invalidReference("weatherRuleId", "天气规则不存在: $weatherRuleId")
		if (weatherRule.code == "clear") {
			invalidValue("weatherRuleId", "跳过蓄力天气不能引用无天气")
		}
	}

	private fun ensureRuleAvailable(request: BattleSkillChargeSkipWeatherRequest, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleSkillChargeSkipWeather::class) {
			where(table.skillRuleId eq request.skillRuleId)
			where(table.weatherRuleId eq request.weatherRuleId)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("weatherRuleId", "该技能规则已经配置相同跳过蓄力天气")
		}
	}

	private fun BattleSkillChargeSkipWeatherRequest.normalized(): BattleSkillChargeSkipWeatherRequest =
		copy(
			skillRuleId = requiredPositiveId(skillRuleId, "skillRuleId"),
			weatherRuleId = requiredPositiveId(weatherRuleId, "weatherRuleId"),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleSkillChargeSkipWeather.toResponse(): BattleSkillChargeSkipWeatherResponse =
		BattleSkillChargeSkipWeatherResponse {
			id = this@toResponse.id
			skillRuleId = this@toResponse.skillRuleId
			weatherRuleId = this@toResponse.weatherRuleId
			enabled = this@toResponse.enabled
			sortOrder = this@toResponse.sortOrder
		}
}
