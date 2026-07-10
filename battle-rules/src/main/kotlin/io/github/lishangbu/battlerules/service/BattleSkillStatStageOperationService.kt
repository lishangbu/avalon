package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleSkillStatStageOperationRequest
import io.github.lishangbu.battlerules.dto.BattleSkillStatStageOperationResponse
import io.github.lishangbu.battlerules.entity.BattleSkillStatStageOperation
import io.github.lishangbu.battlerules.entity.effectTiming
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.operationKind
import io.github.lishangbu.battlerules.entity.skillRuleId
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.sourceScope
import io.github.lishangbu.battlerules.entity.statId
import io.github.lishangbu.battlerules.entity.targetScope
import io.github.lishangbu.battlerules.repository.BattleSkillRuleRepository
import io.github.lishangbu.battlerules.repository.BattleSkillStatStageOperationRepository
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
 * 技能能力阶级操作维护服务。
 *
 * 能力阶级操作用于表达普通 `stageDelta` 无法描述的技能效果：清除已有阶级、复制目标阶级、交换双方阶级、
 * 或把目标阶级取反。本服务把这些规则拆成强类型字段维护，避免用说明文本或策略字符串承载运行时数据。
 *
 * 约束在资料层尽量提前发现：复制和交换必须有来源范围，清除和取反不能配置来源范围，全场目标只允许用于
 * 清除操作。战斗引擎仍会在运行时验证目标是否存在、是否仍可战斗以及是否被替身等规则阻挡。
 */
@Service
class BattleSkillStatStageOperationService(
	private val repository: BattleSkillStatStageOperationRepository,
	private val skillRuleRepository: BattleSkillRuleRepository,
	private val sqlClient: KSqlClient,
) {
	/**
	 * 按技能规则、能力项或操作类型分页查询能力阶级操作。
	 */
	@Transactional(readOnly = true)
	fun list(
		page: Int,
		size: Int,
		skillRuleId: Long?,
		statId: Long?,
		operationKind: String?,
	): Page<BattleSkillStatStageOperationResponse> {
		validatePage(page, size)
		val normalizedKind = operationKind?.takeIf { it.isNotBlank() }?.let { normalizeOperationKind(it) }
		return sqlClient.createQuery(BattleSkillStatStageOperation::class) {
			skillRuleId?.let { where(table.skillRuleId eq requiredPositiveId(it, "skillRuleId")) }
			statId?.let { where(table.statId eq requiredPositiveId(it, "statId")) }
			normalizedKind?.let { where(table.operationKind eq it) }
			orderBy(table.skillRuleId, table.sortOrder, table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	/**
	 * 读取单条技能能力阶级操作。
	 */
	@Transactional(readOnly = true)
	fun get(id: Long): BattleSkillStatStageOperationResponse =
		operationByIdOrNotFound(id).toResponse()

	/**
	 * 创建技能能力阶级操作。
	 */
	@Transactional
	fun create(request: BattleSkillStatStageOperationRequest): BattleSkillStatStageOperationResponse {
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.statId)
		ensureOperationAvailable(normalized, null)
		return repository.save(
			BattleSkillStatStageOperation {
				skillRuleId = normalized.skillRuleId
				statId = normalized.statId
				operationKind = normalized.operationKind
				targetScope = normalized.targetScope
				sourceScope = normalized.sourceScope
				effectTiming = normalized.effectTiming
				chancePercent = normalized.chancePercent
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 更新技能能力阶级操作。
	 */
	@Transactional
	fun update(id: Long, request: BattleSkillStatStageOperationRequest): BattleSkillStatStageOperationResponse {
		operationByIdOrNotFound(id)
		val normalized = request.normalized()
		validateReferences(normalized.skillRuleId, normalized.statId)
		ensureOperationAvailable(normalized, id)
		return repository.save(
			BattleSkillStatStageOperation {
				this.id = id
				skillRuleId = normalized.skillRuleId
				statId = normalized.statId
				operationKind = normalized.operationKind
				targetScope = normalized.targetScope
				sourceScope = normalized.sourceScope
				effectTiming = normalized.effectTiming
				chancePercent = normalized.chancePercent
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 删除技能能力阶级操作。
	 */
	@Transactional
	fun delete(id: Long) {
		operationByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun operationByIdOrNotFound(id: Long): BattleSkillStatStageOperation =
		repository.findNullable(id) ?: notFound("id", "技能能力阶级操作不存在: $id")

	private fun validateReferences(skillRuleId: Long, statId: Long) {
		if (skillRuleRepository.findNullable(skillRuleId) == null) {
			invalidReference("skillRuleId", "技能规则不存在: $skillRuleId")
		}
		requireExistingGameStatReference(sqlClient, statId, "statId", "能力项")
	}

	private fun ensureOperationAvailable(request: BattleSkillStatStageOperationRequest, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleSkillStatStageOperation::class) {
			where(table.skillRuleId eq request.skillRuleId)
			where(table.statId eq request.statId)
			where(table.operationKind eq request.operationKind)
			where(table.targetScope eq request.targetScope)
			where(table.sourceScope eq request.sourceScope)
			where(table.effectTiming eq request.effectTiming)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("statId", "该技能规则已经配置相同能力阶级操作")
		}
	}

	private fun BattleSkillStatStageOperationRequest.normalized(): BattleSkillStatStageOperationRequest {
		val normalizedKind = normalizeOperationKind(operationKind)
		val normalizedTarget = normalizeTargetScope(targetScope, "targetScope")
		val normalizedSource = sourceScope
			?.takeIf { it.isNotBlank() }
			?.let { normalizeTargetScope(it, "sourceScope") }
		validateOperationShape(normalizedKind, normalizedTarget, normalizedSource)
		return copy(
			skillRuleId = requiredPositiveId(skillRuleId, "skillRuleId"),
			statId = requiredPositiveId(statId, "statId"),
			operationKind = normalizedKind,
			targetScope = normalizedTarget,
			sourceScope = normalizedSource,
			effectTiming = requiredEnumText(effectTiming, "effectTiming", EFFECT_TIMINGS),
			chancePercent = requiredIntRange(chancePercent, "chancePercent", 0, 100),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)
	}

	private fun normalizeOperationKind(value: String): String =
		requiredEnumText(value, "operationKind", OPERATION_KINDS)

	private fun normalizeTargetScope(value: String, fieldName: String): String =
		requiredEnumText(value, fieldName, TARGET_SCOPES)

	private fun validateOperationShape(kind: String, target: String, source: String?) {
		when (kind) {
			"CLEAR", "INVERT" -> if (source != null) {
				invalidValue("sourceScope", "$kind 操作不能配置 sourceScope")
			}
			"COPY", "SWAP" -> if (source == null) {
				invalidValue("sourceScope", "$kind 操作必须配置 sourceScope")
			}
		}
		if (target == "ALL_ACTIVE" && kind != "CLEAR") {
			invalidValue("targetScope", "ALL_ACTIVE 只能用于 CLEAR 操作")
		}
		if (source == "ALL_ACTIVE") {
			invalidValue("sourceScope", "sourceScope 不能使用 ALL_ACTIVE")
		}
	}

	private fun BattleSkillStatStageOperation.toResponse(): BattleSkillStatStageOperationResponse =
		BattleSkillStatStageOperationResponse {
			id = this@toResponse.id
			skillRuleId = this@toResponse.skillRuleId
			statId = this@toResponse.statId
			operationKind = this@toResponse.operationKind
			targetScope = this@toResponse.targetScope
			sourceScope = this@toResponse.sourceScope
			effectTiming = this@toResponse.effectTiming
			chancePercent = this@toResponse.chancePercent
			enabled = this@toResponse.enabled
			sortOrder = this@toResponse.sortOrder
		}

	private companion object {
		val OPERATION_KINDS = setOf("CLEAR", "COPY", "SWAP", "INVERT")
		val TARGET_SCOPES = setOf("USER", "TARGET", "ALL_ACTIVE")
		val EFFECT_TIMINGS = setOf("AFTER_HIT")
	}
}
