package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.BattlePreparationValidator
import io.github.lishangbu.battleengine.BattlePreparationViolation
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleFormatSnapshot
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSide
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.model.BattleWeather
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
import java.sql.ResultSet
import org.springframework.jdbc.core.JdbcTemplate
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
	private val jdbcTemplate: JdbcTemplate,
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
		val elementIds = coreElementIds()
		return BattleRuntimeSnapshot(
			format = format.toEngineFormatSnapshot(),
			rules = BattleRuleSnapshot(
				electricElementId = elementIds.requiredElementId("electric"),
				fireElementId = elementIds.requiredElementId("fire"),
				grassElementId = elementIds.requiredElementId("grass"),
				groundElementId = elementIds.requiredElementId("ground"),
				iceElementId = elementIds.requiredElementId("ice"),
				poisonElementId = elementIds.requiredElementId("poison"),
				rockElementId = elementIds.requiredElementId("rock"),
				steelElementId = elementIds.requiredElementId("steel"),
				waterElementId = elementIds.requiredElementId("water"),
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

	/**
	 * 按基础技能 ID 装配战斗引擎可消费的技能槽。
	 *
	 * 装配分为两层：
	 * - 基础事实来自 `game_skill`：名称、属性、伤害分类、威力、命中、PP 和优先度。
	 * - 战斗规则来自 `battle_skill_rule` 及其子表：多段命中、保护交互、天气命中覆盖、天气威力倍率、
	 *   状态附加和能力阶级变化。
	 *
	 * 如果某个基础技能暂时没有显式技能规则，仍会使用引擎默认行为构建技能槽。这个默认只发生在运行时适配层，
	 * 不是通用 CRUD 或原始 JSON fallback；它确保准备校验和早期战斗用例可以消费完整基础资料，同时让需要特殊规则的
	 * 技能逐步由独立规则表覆盖。
	 */
	@Transactional(readOnly = true)
	fun skillSlotsBySkillIds(skillIds: List<Long>): List<BattleSkillSlot> {
		if (skillIds.isEmpty()) {
			invalidValue("skillIds", "skillIds 不能为空")
		}
		return skillIds.map { skillId ->
			if (skillId <= 0) {
				invalidValue("skillIds", "skillIds 只能包含正数 ID")
			}
			skillSlotBySkillId(skillId)
		}
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

	/**
	 * 读取引擎基础规则需要识别的核心属性 ID。
	 *
	 * 这里按稳定 code 而不是硬编码资料 ID 装配快照，保持纯引擎不依赖资料库编号。查询范围只包含当前已由
	 * 伤害、天气和主要异常状态规则使用的属性；后续新增规则需要更多属性时，在这里扩展 code 清单。
	 */
	private fun coreElementIds(): Map<String, Long> =
		jdbcTemplate.query(
			"""
			select code, id
			from game_element
			where code in ('electric', 'fire', 'grass', 'ground', 'ice', 'poison', 'rock', 'steel', 'water')
			""".trimIndent(),
		) { rs, _ -> rs.getString("code") to rs.getLong("id") }.toMap()

	private fun Map<String, Long>.requiredElementId(code: String): Long =
		this[code] ?: error("核心属性资料缺失: $code")

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
			skillSlots = skillSlotsBySkillIds(skillIds),
			abilityId = abilityId,
			itemId = itemId,
		)
	}

	private fun skillSlotBySkillId(skillId: Long): BattleSkillSlot {
		val row = jdbcTemplate.query(
			"""
			select
				s.id as skill_id,
				s.name as skill_name,
				s.element_id,
				dc.code as damage_class_code,
				s.power,
				s.accuracy,
				s.pp,
				s.priority,
				r.id as rule_id,
				r.target_policy,
				r.min_hits,
				r.max_hits,
				r.critical_hit_stage,
				r.makes_contact,
				r.affected_by_protect,
				r.protects_user,
				r.thaws_user_before_move,
				r.powder_based,
				r.weakened_by_grassy_terrain,
				r.lock_move_turns_min,
				r.lock_move_turns_max,
				r.confuses_user_after_lock
			from game_skill s
			join game_skill_damage_class dc on dc.id = s.damage_class_id
			left join battle_skill_rule r on r.skill_id = s.id and r.enabled = true
			where s.id = ?
			""".trimIndent(),
			{ rs, _ -> rs.toSkillRuntimeRow() },
			skillId,
		).singleOrNull() ?: invalidValue("skillIds", "技能不存在: $skillId")

		return BattleSkillSlot(
			skillId = row.skillId,
			name = row.name,
			elementId = row.elementId,
			damageClass = row.damageClassCode.toBattleDamageClass(),
			power = row.power,
			accuracy = row.accuracy,
			targetScope = row.targetPolicy.toBattleSkillTargetScope(),
			minHits = row.minHits ?: 1,
			maxHits = row.maxHits ?: 1,
			makesContact = row.makesContact ?: false,
			criticalHitStage = row.criticalHitStage ?: 0,
			affectedByProtect = row.affectedByProtect ?: true,
			protectsUser = row.protectsUser ?: false,
			thawsUserBeforeMove = row.thawsUserBeforeMove ?: false,
			powderBased = row.powderBased ?: false,
			weakenedByGrassyTerrain = row.weakenedByGrassyTerrain ?: false,
			accuracyOverridesByWeather = weatherAccuracyOverrides(row.ruleId),
			powerMultipliersByWeather = weatherPowerMultipliers(row.ruleId),
			lockMoveTurnsMin = row.lockMoveTurnsMin ?: 1,
			lockMoveTurnsMax = row.lockMoveTurnsMax ?: 1,
			confusesUserAfterLock = row.confusesUserAfterLock ?: false,
			priority = row.priority,
			remainingPp = row.pp,
			maxPp = row.pp,
			statusApplications = statusApplications(row.ruleId),
			volatileStatusApplications = volatileStatusApplications(row.ruleId),
			statStageEffects = statStageEffects(row.ruleId),
		)
	}

	private fun ResultSet.toSkillRuntimeRow(): SkillRuntimeRow =
		SkillRuntimeRow(
			skillId = getLong("skill_id"),
			name = getString("skill_name"),
			elementId = getLong("element_id"),
			damageClassCode = getString("damage_class_code"),
			power = nullableInt("power"),
			accuracy = nullableInt("accuracy"),
			pp = getInt("pp").coerceAtLeast(0),
			priority = getInt("priority"),
			ruleId = nullableLong("rule_id"),
			targetPolicy = getString("target_policy"),
			minHits = nullableInt("min_hits"),
			maxHits = nullableInt("max_hits"),
			criticalHitStage = nullableInt("critical_hit_stage"),
			makesContact = nullableBoolean("makes_contact"),
			affectedByProtect = nullableBoolean("affected_by_protect"),
			protectsUser = nullableBoolean("protects_user"),
			thawsUserBeforeMove = nullableBoolean("thaws_user_before_move"),
			powderBased = nullableBoolean("powder_based"),
			weakenedByGrassyTerrain = nullableBoolean("weakened_by_grassy_terrain"),
			lockMoveTurnsMin = nullableInt("lock_move_turns_min"),
			lockMoveTurnsMax = nullableInt("lock_move_turns_max"),
			confusesUserAfterLock = nullableBoolean("confuses_user_after_lock"),
		)

	private fun weatherAccuracyOverrides(ruleId: Long?): Map<BattleWeather, Int?> {
		if (ruleId == null) {
			return emptyMap()
		}
		return jdbcTemplate.query(
			"""
			select w.code as weather_code, o.accuracy_percent
			from battle_skill_weather_accuracy_override o
			join battle_weather_rule w on w.id = o.weather_rule_id
			where o.skill_rule_id = ? and o.enabled = true and w.enabled = true
			order by o.sort_order, o.id
			""".trimIndent(),
			{ rs, _ -> rs.getString("weather_code").toBattleWeather() to rs.nullableInt("accuracy_percent") },
			ruleId,
		).toMap()
	}

	private fun weatherPowerMultipliers(ruleId: Long?): Map<BattleWeather, Double> {
		if (ruleId == null) {
			return emptyMap()
		}
		return jdbcTemplate.query(
			"""
			select w.code as weather_code, m.power_multiplier
			from battle_skill_weather_power_modifier m
			join battle_weather_rule w on w.id = m.weather_rule_id
			where m.skill_rule_id = ? and m.enabled = true and w.enabled = true
			order by m.sort_order, m.id
			""".trimIndent(),
			{ rs, _ -> rs.getString("weather_code").toBattleWeather() to rs.getDouble("power_multiplier") },
			ruleId,
		).toMap()
	}

	private fun statusApplications(ruleId: Long?): List<BattleStatusApplication> {
		if (ruleId == null) {
			return emptyList()
		}
		return jdbcTemplate.query(
			"""
			select sr.code as status_code, e.target_scope, e.chance_percent
			from battle_skill_status_effect e
			join battle_status_rule sr on sr.id = e.status_rule_id
			where e.skill_rule_id = ?
				and e.enabled = true
				and sr.enabled = true
				and sr.status_kind = 'MAJOR'
				and e.effect_timing = 'AFTER_HIT'
			order by e.sort_order, e.id
			""".trimIndent(),
			{ rs, _ ->
				val target = rs.getString("target_scope").toBattleEffectTarget() ?: return@query null
				BattleStatusApplication(
					status = rs.getString("status_code").toBattleMajorStatus(),
					target = target,
					chancePercent = rs.getInt("chance_percent"),
				)
			},
			ruleId,
		).filterNotNull()
	}

	private fun volatileStatusApplications(ruleId: Long?): List<BattleVolatileStatusApplication> {
		if (ruleId == null) {
			return emptyList()
		}
		return jdbcTemplate.query(
			"""
			select sr.code as status_code, e.target_scope, e.chance_percent
			from battle_skill_status_effect e
			join battle_status_rule sr on sr.id = e.status_rule_id
			where e.skill_rule_id = ?
				and e.enabled = true
				and sr.enabled = true
				and sr.status_kind = 'VOLATILE'
				and e.effect_timing = 'AFTER_HIT'
			order by e.sort_order, e.id
			""".trimIndent(),
			{ rs, _ ->
				val target = rs.getString("target_scope").toBattleEffectTarget() ?: return@query null
				BattleVolatileStatusApplication(
					status = rs.getString("status_code").toBattleVolatileStatus(),
					target = target,
					chancePercent = rs.getInt("chance_percent"),
				)
			},
			ruleId,
		).filterNotNull()
	}

	private fun statStageEffects(ruleId: Long?): List<BattleStatStageEffect> {
		if (ruleId == null) {
			return emptyList()
		}
		return jdbcTemplate.query(
			"""
			select st.code as stat_code, e.target_scope, e.stage_delta, e.chance_percent
			from battle_skill_stat_stage_effect e
			join game_stat st on st.id = e.stat_id
			where e.skill_rule_id = ?
				and e.enabled = true
				and e.effect_timing = 'AFTER_HIT'
			order by e.sort_order, e.id
			""".trimIndent(),
			{ rs, _ ->
				val target = rs.getString("target_scope").toBattleEffectTarget() ?: return@query null
				BattleStatStageEffect(
					stat = rs.getString("stat_code").toBattleStat(),
					target = target,
					stageDelta = rs.getInt("stage_delta"),
					chancePercent = rs.getInt("chance_percent"),
				)
			},
			ruleId,
		).filterNotNull()
	}

	private fun String?.toBattleSkillTargetScope(): BattleSkillTargetScope =
		when (this) {
			"all-opponents" -> BattleSkillTargetScope.ALL_ADJACENT_OPPONENTS
			"all-adjacent-participants" -> BattleSkillTargetScope.ALL_ADJACENT_PARTICIPANTS
			else -> BattleSkillTargetScope.SELECTED_TARGET
		}

	private fun String.toBattleDamageClass(): BattleDamageClass =
		when (this) {
			"physical" -> BattleDamageClass.PHYSICAL
			"special" -> BattleDamageClass.SPECIAL
			"status" -> BattleDamageClass.STATUS
			else -> invalidValue("damageClass", "不支持的技能伤害分类: $this")
		}

	private fun String.toBattleWeather(): BattleWeather =
		when (this) {
			"harsh-sunlight" -> BattleWeather.SUN
			"rain" -> BattleWeather.RAIN
			"sandstorm" -> BattleWeather.SANDSTORM
			"snow" -> BattleWeather.SNOW
			else -> invalidValue("weatherRuleId", "不支持的天气规则: $this")
		}

	private fun String.toBattleEffectTarget(): BattleEffectTarget? =
		when (this) {
			"USER" -> BattleEffectTarget.USER
			"TARGET" -> BattleEffectTarget.TARGET
			// 技能执行器已经按实际命中的目标逐个调用附加效果；资料中的全体对手在这里映射为
			// “当前实际目标”，避免范围技能在效果层再次展开后重复结算。
			"ALL_OPPONENTS" -> BattleEffectTarget.TARGET
			else -> null
		}

	private fun String.toBattleMajorStatus(): BattleMajorStatus =
		when (this) {
			"burn" -> BattleMajorStatus.BURN
			"paralysis" -> BattleMajorStatus.PARALYSIS
			"poison" -> BattleMajorStatus.POISON
			"bad-poison" -> BattleMajorStatus.BAD_POISON
			"sleep" -> BattleMajorStatus.SLEEP
			"freeze" -> BattleMajorStatus.FREEZE
			else -> invalidValue("statusRuleId", "不支持的主要异常状态: $this")
		}

	private fun String.toBattleVolatileStatus(): BattleVolatileStatus =
		when (this) {
			"confusion" -> BattleVolatileStatus.CONFUSION
			"flinch" -> BattleVolatileStatus.FLINCH
			else -> invalidValue("statusRuleId", "不支持的临时状态: $this")
		}

	private fun String.toBattleStat(): BattleStat =
		when (this) {
			"attack" -> BattleStat.ATTACK
			"defense" -> BattleStat.DEFENSE
			"special-attack" -> BattleStat.SPECIAL_ATTACK
			"special-defense" -> BattleStat.SPECIAL_DEFENSE
			"speed" -> BattleStat.SPEED
			"accuracy" -> BattleStat.ACCURACY
			"evasion" -> BattleStat.EVASION
			else -> invalidValue("statId", "不支持的战斗能力项: $this")
		}

	private fun ResultSet.nullableInt(column: String): Int? {
		val value = getInt(column)
		return if (wasNull()) null else value
	}

	private fun ResultSet.nullableLong(column: String): Long? {
		val value = getLong(column)
		return if (wasNull()) null else value
	}

	private fun ResultSet.nullableBoolean(column: String): Boolean? {
		val value = getBoolean(column)
		return if (wasNull()) null else value
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

private data class SkillRuntimeRow(
	val skillId: Long,
	val name: String,
	val elementId: Long,
	val damageClassCode: String,
	val power: Int?,
	val accuracy: Int?,
	val pp: Int,
	val priority: Int,
	val ruleId: Long?,
	val targetPolicy: String?,
	val minHits: Int?,
	val maxHits: Int?,
	val criticalHitStage: Int?,
	val makesContact: Boolean?,
	val affectedByProtect: Boolean?,
	val protectsUser: Boolean?,
	val thawsUserBeforeMove: Boolean?,
	val powderBased: Boolean?,
	val weakenedByGrassyTerrain: Boolean?,
	val lockMoveTurnsMin: Int?,
	val lockMoveTurnsMax: Int?,
	val confusesUserAfterLock: Boolean?,
)
