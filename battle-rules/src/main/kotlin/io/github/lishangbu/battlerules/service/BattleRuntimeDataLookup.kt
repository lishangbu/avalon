package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderApplication
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderEffect
import io.github.lishangbu.battleengine.model.BattleFormatSnapshot
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSideConditionApplication
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
import io.github.lishangbu.battleengine.model.BattleSideEntryHazard
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardApplication
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifier
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierApplication
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatStageOperation
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.model.BattleWeather
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
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet

private const val DEFAULT_INDIVIDUAL_VALUE = 31
private const val DEFAULT_EFFORT_VALUE = 0

/**
 * 战斗运行时资料读取器。
 *
 * 本类集中承接所有数据库读取、Jimmer 查询和 ResultSet 行转换，把 Spring/JDBC 细节留在 battle-rules 适配层内。
 * [BattleRuntimeSnapshotService] 只通过这里拿到已经翻译成 battle-engine 强类型模型的资料，避免服务层同时承担请求校验、
 * 事务编排、SQL 文本和策略映射四类职责。这里不保存跨请求缓存；管理端修改资料后，下一个事务会自然读取最新数据。
 */
@Component
class BattleRuntimeDataLookup(
	private val sqlClient: KSqlClient,
	private val jdbcTemplate: JdbcTemplate,
) {
	fun runtimeSnapshotByFormatCode(formatCode: String, elementIds: Map<String, Long>): BattleRuntimeSnapshot {
		val format = formatByCode(formatCode)
		val restrictions = enabledRestrictions(format.id)
		val clauseCodes = enabledClauseCodes(format.id)
		return BattleRuntimeSnapshot(
			format = format.toEngineFormatSnapshot(),
			rules = BattleRuleSnapshot(
				darkElementId = elementIds.requiredElementId("dark"),
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
 * 这里按稳定 code 而不是硬编码资料 ID 装配快照，保持纯引擎不依赖资料库编号。查询范围覆盖现代主系列 18 个
 * 常规属性，因为伤害、天气、主要异常状态、属性限定特性和属性伤害提升道具都可能需要把资料 policy 翻译成
 * 引擎可执行的属性 ID。
 */
fun coreElementIds(): Map<String, Long> =
	jdbcTemplate.query(
		"""
		select code, id
		from game_element
		where code in (
			'normal', 'fighting', 'flying', 'poison', 'ground', 'rock',
			'bug', 'ghost', 'steel', 'fire', 'water', 'grass',
			'electric', 'psychic', 'ice', 'dragon', 'dark', 'fairy'
		)
		""".trimIndent(),
	) { rs, _ -> rs.getString("code") to rs.getLong("id") }.toMap()

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
					?.split(',', ';', ' ', '\n', '	')
					.orEmpty()
					.mapNotNull { it.trim().toLongOrNull() }
					.filter { it > 0 }
					.forEach(::add)
			}
		}
		.toSet()

/**
 * 按基础成员资料和等级读取引擎需要的能力值与属性集合。
 *
 * 这里保留第一版运行时 DTO 的中性假设：个体值固定 31、努力值固定 0、性格修正固定 1.0。这样读取器返回的是
 * 可以直接放入 [io.github.lishangbu.battleengine.model.BattleParticipant] 的稳定运行时画像，而不是数据库行。
 * 后续如果请求 DTO 支持个体值、努力值和性格，应扩展本方法入参和公式参数，不应让纯引擎回头访问数据库。
 */
fun creatureRuntimeProfile(creatureId: Long, level: Int): BattleCreatureRuntimeProfile {
	if (creatureId <= 0) {
		invalidValue("creatureId", "creatureId 必须大于 0")
	}
	if (level !in 1..100) {
		invalidValue("level", "level 必须在 1 到 100 之间")
	}
	val elementIds = jdbcTemplate.query(
		"""
		select element_id
		from game_creature_element
		where creature_id = ?
		order by slot_order, id
		""".trimIndent(),
		{ rs, _ -> rs.getLong("element_id") },
		creatureId,
	)
	if (elementIds.isEmpty()) {
		notFound("creatureId", "成员属性资料不存在: $creatureId")
	}
	val baseStats = jdbcTemplate.query(
		"""
		select s.code, cs.base_value
		from game_creature_stat cs
		join game_stat s on s.id = cs.stat_id
		where cs.creature_id = ?
		""".trimIndent(),
		{ rs, _ -> rs.getString("code") to rs.getInt("base_value") },
		creatureId,
	).toMap()
	if (baseStats.isEmpty()) {
		notFound("creatureId", "成员能力资料不存在: $creatureId")
	}
	return BattleCreatureRuntimeProfile(
		maxHp = baseStats.requiredBaseStat("hp").toRuntimeHp(level),
		attack = baseStats.requiredBaseStat("attack").toRuntimeBattleStat(level),
		defense = baseStats.requiredBaseStat("defense").toRuntimeBattleStat(level),
		specialAttack = baseStats.requiredBaseStat("special-attack").toRuntimeBattleStat(level),
		specialDefense = baseStats.requiredBaseStat("special-defense").toRuntimeBattleStat(level),
		speed = baseStats.requiredBaseStat("speed").toRuntimeBattleStat(level),
		elementIds = elementIds.toSet(),
	)
}

fun enabledAbilityPolicies(abilityId: Long): List<String> =
	jdbcTemplate.query(
		"""
		select effect_policy
		from battle_ability_rule
		where ability_id = ? and enabled = true
		order by trigger_order, sort_order, id
		""".trimIndent(),
		{ rs, _ -> rs.getString("effect_policy") },
		abilityId,
	)

fun enabledItemPolicies(itemId: Long): List<String> =
	jdbcTemplate.query(
		"""
		select effect_policy
		from battle_item_rule
		where item_id = ? and enabled = true
		order by trigger_order, sort_order, id
		""".trimIndent(),
		{ rs, _ -> rs.getString("effect_policy") },
		itemId,
	)

fun skillSlotBySkillId(skillId: Long): BattleSkillSlot {
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
			r.effect_policy,
			r.target_policy,
			r.min_hits,
			r.max_hits,
			r.critical_hit_stage,
			r.makes_contact,
			r.affected_by_protect,
			r.protects_user,
			r.thaws_user_before_move,
			r.sound_based,
			r.powder_based,
			r.punch_based,
			r.slicing_based,
			r.weakened_by_grassy_terrain,
			r.charges_before_use,
			r.recharges_after_use,
			r.lock_move_turns_min,
			r.lock_move_turns_max,
			r.confuses_user_after_lock,
			r.force_target_switch
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
		fixedDamage = row.effectPolicy.toBattleFixedDamage(),
		proportionalDamage = row.effectPolicy.toBattleProportionalDamage(),
		hpDerivedDamage = row.effectPolicy.toBattleHpDerivedDamage(),
		accuracy = row.accuracy,
		targetScope = row.targetPolicy.toBattleSkillTargetScope(),
		minHits = row.minHits ?: 1,
		maxHits = row.maxHits ?: 1,
		makesContact = row.makesContact ?: false,
		criticalHitStage = row.criticalHitStage ?: 0,
		affectedByProtect = row.affectedByProtect ?: true,
		protectsUser = row.protectsUser ?: false,
		thawsUserBeforeMove = row.thawsUserBeforeMove ?: false,
		soundBased = row.soundBased ?: false,
		powderBased = row.powderBased ?: false,
		punchBased = row.punchBased ?: false,
		slicingBased = row.slicingBased ?: false,
		weakenedByGrassyTerrain = row.weakenedByGrassyTerrain ?: false,
		chargesBeforeUse = row.chargesBeforeUse ?: false,
		chargeSkippedByWeathers = chargeSkippedByWeathers(row.ruleId),
		rechargesAfterUse = row.rechargesAfterUse ?: false,
		accuracyOverridesByWeather = weatherAccuracyOverrides(row.ruleId),
		powerMultipliersByWeather = weatherPowerMultipliers(row.ruleId),
		elementOverridesByWeather = weatherElementOverrides(row.ruleId),
		lockMoveTurnsMin = row.lockMoveTurnsMin ?: 1,
		lockMoveTurnsMax = row.lockMoveTurnsMax ?: 1,
		confusesUserAfterLock = row.confusesUserAfterLock ?: false,
		forceTargetSwitch = row.forceTargetSwitch ?: false,
		priority = row.priority,
		remainingPp = row.pp,
		maxPp = row.pp,
		statusApplications = statusApplications(row.ruleId),
		volatileStatusApplications = volatileStatusApplications(row.ruleId),
		statStageEffects = statStageEffects(row.ruleId),
		statStageOperations = statStageOperations(row.ruleId),
		sideConditionApplications = sideConditionApplications(row.ruleId),
		sideSpeedModifierApplications = sideSpeedModifierApplications(row.ruleId),
		sideEntryHazardApplications = sideEntryHazardApplications(row.ruleId),
		fieldSpeedOrderApplications = fieldSpeedOrderApplications(row.ruleId),
		hpEffects = row.effectPolicy.toBattleSkillHpEffects(),
		environmentEffects = row.effectPolicy.toBattleSkillEnvironmentEffects(),
	)
}

private fun ResultSet.toSkillRuntimeRow(): SkillRuntimeRow =
	SkillRuntimeRow(
		skillId = getLong("skill_id"),
		name = getString("skill_name"),
		elementId = getLong("element_id"),
		damageClassCode = getString("damage_class_code"),
		power = nullableInt("power")?.takeIf { it > 0 },
		accuracy = nullableInt("accuracy")?.takeIf { it > 0 },
		pp = getInt("pp").coerceAtLeast(0),
		priority = getInt("priority"),
		ruleId = nullableLong("rule_id"),
		effectPolicy = getString("effect_policy"),
		targetPolicy = getString("target_policy"),
		minHits = nullableInt("min_hits"),
		maxHits = nullableInt("max_hits"),
		criticalHitStage = nullableInt("critical_hit_stage"),
		makesContact = nullableBoolean("makes_contact"),
		affectedByProtect = nullableBoolean("affected_by_protect"),
		protectsUser = nullableBoolean("protects_user"),
		thawsUserBeforeMove = nullableBoolean("thaws_user_before_move"),
		soundBased = nullableBoolean("sound_based"),
		powderBased = nullableBoolean("powder_based"),
		punchBased = nullableBoolean("punch_based"),
		slicingBased = nullableBoolean("slicing_based"),
		weakenedByGrassyTerrain = nullableBoolean("weakened_by_grassy_terrain"),
		chargesBeforeUse = nullableBoolean("charges_before_use"),
		rechargesAfterUse = nullableBoolean("recharges_after_use"),
		lockMoveTurnsMin = nullableInt("lock_move_turns_min"),
		lockMoveTurnsMax = nullableInt("lock_move_turns_max"),
		confusesUserAfterLock = nullableBoolean("confuses_user_after_lock"),
		forceTargetSwitch = nullableBoolean("force_target_switch"),
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

private fun weatherElementOverrides(ruleId: Long?): Map<BattleWeather, Long> {
	if (ruleId == null) {
		return emptyMap()
	}
	return jdbcTemplate.query(
		"""
		select w.code as weather_code, o.target_element_id
		from battle_skill_weather_element_override o
		join battle_weather_rule w on w.id = o.weather_rule_id
		join game_element e on e.id = o.target_element_id
		where o.skill_rule_id = ? and o.enabled = true and w.enabled = true and e.enabled = true
		order by o.sort_order, o.id
		""".trimIndent(),
		{ rs, _ -> rs.getString("weather_code").toBattleWeather() to rs.getLong("target_element_id") },
		ruleId,
	).toMap()
}

private fun chargeSkippedByWeathers(ruleId: Long?): Set<BattleWeather> {
	if (ruleId == null) {
		return emptySet()
	}
	return jdbcTemplate.query(
		"""
		select w.code as weather_code
		from battle_skill_charge_skip_weather s
		join battle_weather_rule w on w.id = s.weather_rule_id
		where s.skill_rule_id = ? and s.enabled = true and w.enabled = true
		order by s.sort_order, s.id
		""".trimIndent(),
		{ rs, _ -> rs.getString("weather_code").toBattleWeather() },
		ruleId,
	).toSet()
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

private fun statStageOperations(ruleId: Long?): List<BattleStatStageOperation> {
	if (ruleId == null) {
		return emptyList()
	}
	return jdbcTemplate.query(
		"""
		select
			st.code as stat_code,
			e.operation_kind,
			e.target_scope,
			e.source_scope,
			e.chance_percent
		from battle_skill_stat_stage_operation e
		join game_stat st on st.id = e.stat_id
		where e.skill_rule_id = ?
			and e.enabled = true
			and e.effect_timing = 'AFTER_HIT'
		order by e.sort_order, e.id
		""".trimIndent(),
		{ rs, _ ->
			val source = rs.getString("source_scope")?.toBattleStatStageOperationTarget()
			BattleStatStageOperation(
				kind = rs.getString("operation_kind").toBattleStatStageOperationKind(),
				stat = rs.getString("stat_code").toBattleStat(),
				target = rs.getString("target_scope").toBattleStatStageOperationTarget(),
				source = source,
				chancePercent = rs.getInt("chance_percent"),
			)
		},
		ruleId,
	)
}

private fun sideConditionApplications(ruleId: Long?): List<BattleSideConditionApplication> {
	if (ruleId == null) {
		return emptyList()
	}
	return jdbcTemplate.query(
		"""
		select
			fr.effect_policy as field_effect_policy,
			fr.min_turns,
			e.target_side,
			e.chance_percent,
			w.code as required_weather_code
		from battle_skill_field_effect e
		join battle_field_rule fr on fr.id = e.field_rule_id
		left join battle_weather_rule w on w.id = e.required_weather_rule_id
		where e.skill_rule_id = ?
			and e.enabled = true
			and fr.enabled = true
			and fr.effect_scope = 'SIDE'
			and e.effect_timing = 'AFTER_HIT'
		order by e.sort_order, e.id
		""".trimIndent(),
		{ rs, _ ->
			val reductionKind = rs.getString("field_effect_policy").toBattleSideDamageReductionKind() ?: return@query null
			BattleSideConditionApplication(
				targetSide = rs.getString("target_side").toBattleSideConditionTarget(),
				damageReduction = BattleSideDamageReduction(
					kind = reductionKind,
					turnsRemaining = rs.nullableInt("min_turns"),
				),
				chancePercent = rs.getInt("chance_percent"),
				requiredWeather = rs.getString("required_weather_code")?.toBattleWeather(),
			)
		},
		ruleId,
	).filterNotNull()
}

private fun sideSpeedModifierApplications(ruleId: Long?): List<BattleSideSpeedModifierApplication> {
	if (ruleId == null) {
		return emptyList()
	}
	return jdbcTemplate.query(
		"""
		select
			fr.effect_policy as field_effect_policy,
			fr.min_turns,
			e.target_side,
			e.chance_percent,
			w.code as required_weather_code
		from battle_skill_field_effect e
		join battle_field_rule fr on fr.id = e.field_rule_id
		left join battle_weather_rule w on w.id = e.required_weather_rule_id
		where e.skill_rule_id = ?
			and e.enabled = true
			and fr.enabled = true
			and fr.effect_scope = 'SIDE'
			and e.effect_timing = 'AFTER_HIT'
		order by e.sort_order, e.id
		""".trimIndent(),
		{ rs, _ ->
			val modifierKind = rs.getString("field_effect_policy").toBattleSideSpeedModifierKind() ?: return@query null
			BattleSideSpeedModifierApplication(
				targetSide = rs.getString("target_side").toBattleSideConditionTarget(),
				speedModifier = BattleSideSpeedModifier(
					kind = modifierKind,
					turnsRemaining = rs.nullableInt("min_turns"),
				),
				chancePercent = rs.getInt("chance_percent"),
				requiredWeather = rs.getString("required_weather_code")?.toBattleWeather(),
			)
		},
		ruleId,
	).filterNotNull()
}

private fun sideEntryHazardApplications(ruleId: Long?): List<BattleSideEntryHazardApplication> {
	if (ruleId == null) {
		return emptyList()
	}
	return jdbcTemplate.query(
		"""
		select
			fr.effect_policy as field_effect_policy,
			fr.max_layers,
			e.target_side,
			e.chance_percent,
			w.code as required_weather_code
		from battle_skill_field_effect e
		join battle_field_rule fr on fr.id = e.field_rule_id
		left join battle_weather_rule w on w.id = e.required_weather_rule_id
		where e.skill_rule_id = ?
			and e.enabled = true
			and fr.enabled = true
			and fr.effect_scope = 'SIDE'
			and e.effect_timing = 'AFTER_HIT'
		order by e.sort_order, e.id
		""".trimIndent(),
		{ rs, _ ->
			val hazardKind = rs.getString("field_effect_policy").toBattleSideEntryHazardKind() ?: return@query null
			BattleSideEntryHazardApplication(
				targetSide = rs.getString("target_side").toBattleSideConditionTarget(),
				hazard = BattleSideEntryHazard(
					kind = hazardKind,
					maxLayers = rs.nullableInt("max_layers") ?: hazardKind.defaultMaxLayers,
				),
				chancePercent = rs.getInt("chance_percent"),
				requiredWeather = rs.getString("required_weather_code")?.toBattleWeather(),
			)
		},
		ruleId,
	).filterNotNull()
}

private fun fieldSpeedOrderApplications(ruleId: Long?): List<BattleFieldSpeedOrderApplication> {
	if (ruleId == null) {
		return emptyList()
	}
	return jdbcTemplate.query(
		"""
		select
			fr.effect_policy as field_effect_policy,
			fr.min_turns,
			e.chance_percent,
			w.code as required_weather_code
		from battle_skill_global_field_effect e
		join battle_field_rule fr on fr.id = e.field_rule_id
		left join battle_weather_rule w on w.id = e.required_weather_rule_id
		where e.skill_rule_id = ?
			and e.enabled = true
			and fr.enabled = true
			and fr.effect_scope = 'FIELD'
			and e.effect_timing = 'AFTER_HIT'
		order by e.sort_order, e.id
		""".trimIndent(),
		{ rs, _ ->
			val speedOrderKind = rs.getString("field_effect_policy").toBattleFieldSpeedOrderKind() ?: return@query null
			BattleFieldSpeedOrderApplication(
				speedOrderEffect = BattleFieldSpeedOrderEffect(
					kind = speedOrderKind,
					turnsRemaining = rs.nullableInt("min_turns"),
				),
				chancePercent = rs.getInt("chance_percent"),
				requiredWeather = rs.getString("required_weather_code")?.toBattleWeather(),
			)
		},
		ruleId,
	).filterNotNull()
}

private fun Map<String, Int>.requiredBaseStat(code: String): Int =
	this[code] ?: notFound("creatureId", "成员基础能力缺失: $code")

private fun Int.toRuntimeHp(level: Int): Int =
	(((2 * this + DEFAULT_INDIVIDUAL_VALUE + DEFAULT_EFFORT_VALUE / 4) * level) / 100) + level + 10

private fun Int.toRuntimeBattleStat(level: Int): Int =
	(((2 * this + DEFAULT_INDIVIDUAL_VALUE + DEFAULT_EFFORT_VALUE / 4) * level) / 100) + 5

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
}

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
	val effectPolicy: String?,
	val targetPolicy: String?,
	val minHits: Int?,
	val maxHits: Int?,
	val criticalHitStage: Int?,
	val makesContact: Boolean?,
	val affectedByProtect: Boolean?,
	val protectsUser: Boolean?,
	val thawsUserBeforeMove: Boolean?,
	val soundBased: Boolean?,
	val powderBased: Boolean?,
	val punchBased: Boolean?,
	val slicingBased: Boolean?,
	val weakenedByGrassyTerrain: Boolean?,
	val chargesBeforeUse: Boolean?,
	val rechargesAfterUse: Boolean?,
	val lockMoveTurnsMin: Int?,
	val lockMoveTurnsMax: Int?,
	val confusesUserAfterLock: Boolean?,
	val forceTargetSwitch: Boolean?,
)
