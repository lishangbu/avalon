package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.BattlePreparationValidator
import io.github.lishangbu.battleengine.BattlePreparationViolation
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleFormatSnapshot
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSide
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battlerules.dto.BattlePreparationParticipantRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationSideRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationValidationRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationValidationResponse
import io.github.lishangbu.battlerules.dto.BattlePreparationViolationResponse
import io.github.lishangbu.battlerules.entity.BattleFormat
import io.github.lishangbu.battlerules.entity.BattleFormatClause
import io.github.lishangbu.battlerules.entity.BattleFormatClauseBinding
import io.github.lishangbu.battlerules.entity.BattleFormatRestriction
import io.github.lishangbu.battlerules.entity.activeParticipantCount
import io.github.lishangbu.battlerules.entity.battleMode
import io.github.lishangbu.battlerules.entity.clauseId
import io.github.lishangbu.battlerules.entity.code
import io.github.lishangbu.battlerules.entity.defaultLevel
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.formatId
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.operandNumber
import io.github.lishangbu.battlerules.entity.operandText
import io.github.lishangbu.battlerules.entity.playerCount
import io.github.lishangbu.battlerules.entity.restrictionOperator
import io.github.lishangbu.battlerules.entity.restrictionType
import io.github.lishangbu.battlerules.entity.teamSize
import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredText
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 战斗运行时规则快照装配服务。
 *
 * 该服务是 battle-rules 管理资料和 battle-engine 纯领域模型之间的适配层。它只读取已经启用的赛制条款
 * 和限制记录，并把它们转换成引擎可直接消费的结构化快照；不执行队伍校验，也不启动战斗。
 *
 * 当前解释的资料约定：
 * - `LEVEL/MAX` 的 `operandNumber` 转换为成员等级上限。
 * - `CREATURE/SKILL/ABILITY/ITEM` + `BAN` 转换为对应禁用 ID 集合。
 * - `species-unique` 条款转换为队伍内种类唯一。
 * - `item-unique` 条款转换为队伍内道具唯一。
 *
 * 未识别的条款或限制会被忽略，便于资料先行维护；真正需要引擎执行的新规则应在这里显式补充映射。
 */
@Service
class BattleRuntimeSnapshotService(
	private val sqlClient: KSqlClient,
) {
	private val preparationValidator = BattlePreparationValidator()

	/**
	 * 按赛制 code 装配运行时快照。
	 */
	@Transactional(readOnly = true)
	fun getByFormatCode(formatCode: String): BattleRuntimeSnapshot {
		val format = formatByCode(formatCode)
		val restrictions = enabledRestrictions(format.id)
		val clauseCodes = enabledClauseCodes(format.id)
		return BattleRuntimeSnapshot(
			format = format.toEngineFormatSnapshot(),
			rules = BattleRuleSnapshot(
				maxParticipantLevel = restrictions
					.filter { it.restrictionType == "LEVEL" && it.restrictionOperator == "MAX" }
					.mapNotNull { it.operandNumber }
					.minOrNull(),
				bannedCreatureIds = restrictions.bannedIds("CREATURE"),
				bannedSkillIds = restrictions.bannedIds("SKILL"),
				bannedAbilityIds = restrictions.bannedIds("ABILITY"),
				bannedItemIds = restrictions.bannedIds("ITEM"),
				uniqueCreatureRequired = "species-unique" in clauseCodes,
				uniqueItemRequired = "item-unique" in clauseCodes,
			),
		)
	}

	/**
	 * 按赛制 code 校验准备阶段队伍。
	 *
	 * 该方法先装配运行时规则快照，再把请求中的轻量队伍资料转换成引擎初始快照，最后调用纯引擎校验器。
	 * 它不查询成员、技能、特性或道具资料表；ID 是否存在应由上层构建队伍时保证，当前校验只判断赛制是否合法。
	 */
	@Transactional(readOnly = true)
	fun validatePreparation(request: BattlePreparationValidationRequest): BattlePreparationValidationResponse {
		val normalized = request.normalized()
		val runtime = getByFormatCode(normalized.formatCode)
		val initialState = BattleInitialState(
			format = runtime.format,
			rules = runtime.rules,
			sides = normalized.sides.map { it.toBattleSide() },
		)
		val violations = preparationValidator.validate(initialState)
		return BattlePreparationValidationResponse(
			valid = violations.isEmpty(),
			violations = violations.map { it.toResponse() },
		)
	}

	private fun formatByCode(formatCode: String): BattleFormat =
		sqlClient.executeQuery(BattleFormat::class) {
			where(table.code eq formatCode)
			select(table)
		}.singleOrNull() ?: notFound("formatCode", "战斗赛制不存在: $formatCode")

	private fun enabledRestrictions(formatId: Long): List<BattleFormatRestriction> =
		sqlClient.executeQuery(BattleFormatRestriction::class) {
			where(table.formatId eq formatId)
			where(table.enabled eq true)
			select(table)
		}

	private fun enabledClauseCodes(formatId: Long): Set<String> {
		val clauseIds = sqlClient.executeQuery(BattleFormatClauseBinding::class) {
			where(table.formatId eq formatId)
			select(table.clauseId)
		}
		if (clauseIds.isEmpty()) {
			return emptySet()
		}
		return sqlClient.executeQuery(BattleFormatClause::class) {
			where(table.id valueIn clauseIds)
			where(table.enabled eq true)
			select(table.code)
		}.toSet()
	}

	private fun BattleFormat.toEngineFormatSnapshot(): BattleFormatSnapshot =
		BattleFormatSnapshot(
			code = code,
			mode = BattleMode.valueOf(battleMode),
			activeParticipantsPerSide = activeParticipantCount,
			playerCount = playerCount,
			teamSize = teamSize,
			defaultLevel = defaultLevel,
		)

	private fun List<BattleFormatRestriction>.bannedIds(type: String): Set<Long> =
		filter { it.restrictionType == type && it.restrictionOperator == "BAN" }
			.flatMap { restriction ->
				buildList {
					restriction.operandNumber?.takeIf { it > 0 }?.let { add(it.toLong()) }
					restriction.operandText
						?.split(',', ';', ' ', '\n', '\t')
						.orEmpty()
						.mapNotNull { it.trim().toLongOrNull() }
						.filter { it > 0 }
						.forEach(::add)
				}
			}
			.toSet()

	private fun BattlePreparationValidationRequest.normalized(): BattlePreparationValidationRequest =
		copy(
			formatCode = formatCode.requiredText("formatCode", maxLength = 80),
			sides = sides.takeIf { it.isNotEmpty() } ?: invalidValue("sides", "sides 不能为空"),
		)

	private fun BattlePreparationSideRequest.toBattleSide(): BattleSide {
		val normalizedSideId = sideId.requiredText("sideId", maxLength = 80)
		if (activeActorIds.isEmpty()) {
			invalidValue("activeActorIds", "activeActorIds 不能为空")
		}
		if (participants.isEmpty()) {
			invalidValue("participants", "participants 不能为空")
		}
		return BattleSide(
			sideId = normalizedSideId,
			activeActorIds = activeActorIds.map { it.requiredText("activeActorIds", maxLength = 80) },
			participants = participants.map { it.toBattleParticipant() },
		)
	}

	private fun BattlePreparationParticipantRequest.toBattleParticipant(): BattleParticipant {
		val normalizedActorId = actorId.requiredText("actorId", maxLength = 80)
		if (creatureId <= 0) {
			invalidValue("creatureId", "creatureId 必须大于 0")
		}
		if (level !in 1..100) {
			invalidValue("level", "level 必须在 1 到 100 之间")
		}
		if (skillIds.isEmpty()) {
			invalidValue("skillIds", "skillIds 不能为空")
		}
		abilityId?.takeIf { it <= 0 }?.let {
			invalidValue("abilityId", "abilityId 必须大于 0")
		}
		itemId?.takeIf { it <= 0 }?.let {
			invalidValue("itemId", "itemId 必须大于 0")
		}
		return BattleParticipant(
			actorId = normalizedActorId,
			creatureId = creatureId,
			level = level,
			maxHp = 1,
			currentHp = 1,
			attack = 1,
			defense = 1,
			specialAttack = 1,
			specialDefense = 1,
			speed = 1,
			elementIds = setOf(1),
			skillSlots = skillIds.map { it.toPreparationSkillSlot() },
			abilityId = abilityId,
			itemId = itemId,
		)
	}

	private fun Long.toPreparationSkillSlot(): BattleSkillSlot {
		if (this <= 0) {
			invalidValue("skillIds", "skillIds 只能包含正数 ID")
		}
		return BattleSkillSlot(
			skillId = this,
			name = "准备校验技能-$this",
			elementId = 1,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			accuracy = null,
			remainingPp = 1,
			maxPp = 1,
		)
	}

	private fun BattlePreparationViolation.toResponse(): BattlePreparationViolationResponse =
		BattlePreparationViolationResponse(
			code = code,
			sideId = sideId,
			actorId = actorId,
			resourceId = resourceId,
			message = message,
		)
}

/**
 * battle-rules 装配出的引擎运行时快照。
 *
 * `format` 描述站位、队伍规模和等级拉平；`rules` 描述准备阶段限制和战斗内基础规则。
 */
data class BattleRuntimeSnapshot(
	val format: BattleFormatSnapshot,
	val rules: BattleRuleSnapshot,
)
