package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderApplication
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderEffect
import io.github.lishangbu.battleengine.model.BattleSideConditionApplication
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
import io.github.lishangbu.battleengine.model.BattleSideEntryHazard
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardApplication
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifier
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierApplication
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatStageOperation
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.model.BattleWeather
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet

/**
 * 技能规则效果子表运行时读取器。
 *
 * `game_skill` 和 `battle_skill_rule` 主行只描述“这个技能是什么”以及“使用哪类运行时 policy”。真正需要多行维护的
 * 明细，例如天气命中修正、天气属性覆盖、主要状态附加、能力阶级变化、一侧场地状态和全场速度顺序效果，都落在
 * `battle_skill_rule.id` 下面的子表。本类专门读取这些子表，并一次性返回 [BattleSkillRuleEffectRuntimeSnapshot]。
 *
 * 这里不读取技能名称、PP、威力、目标 policy，也不校验 policy 是否被支持；这些仍然属于 [BattleSkillRuntimeLookup]
 * 的主行装配职责。这样分开后，新增一个子表效果只会改动本类和对应快照测试；新增一个主行布尔字段或 policy 校验也
 * 不会让子表 SQL 跟着一起膨胀。
 *
 * `ruleId == null` 表示资料库中没有启用的 `battle_skill_rule` 行。这类普通技能应该继续使用 battle-engine 的默认
 * 技能槽行为，因此本类会返回全空快照，而不是抛错或查询每张子表。这个约定让“没有规则行”和“有规则行但子表为空”
 * 在运行时表现一致，也避免普通技能为所有可选效果付出额外 SQL 成本。
 */
@Component
class BattleSkillRuleEffectRuntimeLookup(
	private val jdbcTemplate: JdbcTemplate,
) {
	/**
	 * 读取某条技能规则挂载的全部多行效果。
	 *
	 * 返回值按 battle-engine 的强类型模型组织，而不是暴露数据库行。这样上层装配 [BattleSkillRuntimeLookup] 只需要把
	 * 快照字段复制进 `BattleSkillSlot`，不会关心某个效果来自天气表、状态表、能力阶级表还是场地表。
	 */
	fun ruleEffects(ruleId: Long?): BattleSkillRuleEffectRuntimeSnapshot {
		if (ruleId == null) {
			return BattleSkillRuleEffectRuntimeSnapshot.EMPTY
		}
		return BattleSkillRuleEffectRuntimeSnapshot(
			chargeSkippedByWeathers = chargeSkippedByWeathers(ruleId),
			accuracyOverridesByWeather = weatherAccuracyOverrides(ruleId),
			powerMultipliersByWeather = weatherPowerMultipliers(ruleId),
			elementOverridesByWeather = weatherElementOverrides(ruleId),
			statusApplications = statusApplications(ruleId),
			volatileStatusApplications = volatileStatusApplications(ruleId),
			statStageEffects = statStageEffects(ruleId),
			statStageOperations = statStageOperations(ruleId),
			sideConditionApplications = sideConditionApplications(ruleId),
			sideSpeedModifierApplications = sideSpeedModifierApplications(ruleId),
			sideEntryHazardApplications = sideEntryHazardApplications(ruleId),
			fieldSpeedOrderApplications = fieldSpeedOrderApplications(ruleId),
		)
	}

	private fun weatherAccuracyOverrides(ruleId: Long): Map<BattleWeather, Int?> =
		jdbcTemplate.query(
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

	private fun weatherPowerMultipliers(ruleId: Long): Map<BattleWeather, Double> =
		jdbcTemplate.query(
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

	private fun weatherElementOverrides(ruleId: Long): Map<BattleWeather, Long> =
		jdbcTemplate.query(
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

	private fun chargeSkippedByWeathers(ruleId: Long): Set<BattleWeather> =
		jdbcTemplate.query(
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

	private fun statusApplications(ruleId: Long): List<BattleStatusApplication> =
		jdbcTemplate.query(
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

	private fun volatileStatusApplications(ruleId: Long): List<BattleVolatileStatusApplication> =
		jdbcTemplate.query(
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

	private fun statStageEffects(ruleId: Long): List<BattleStatStageEffect> =
		jdbcTemplate.query(
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

	private fun statStageOperations(ruleId: Long): List<BattleStatStageOperation> =
		jdbcTemplate.query(
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

	private fun sideConditionApplications(ruleId: Long): List<BattleSideConditionApplication> =
		jdbcTemplate.query(
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

	private fun sideSpeedModifierApplications(ruleId: Long): List<BattleSideSpeedModifierApplication> =
		jdbcTemplate.query(
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

	private fun sideEntryHazardApplications(ruleId: Long): List<BattleSideEntryHazardApplication> =
		jdbcTemplate.query(
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

	private fun fieldSpeedOrderApplications(ruleId: Long): List<BattleFieldSpeedOrderApplication> =
		jdbcTemplate.query(
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

	private fun ResultSet.nullableInt(column: String): Int? {
		val value = getInt(column)
		return if (wasNull()) null else value
	}
}

/**
 * 一条技能规则所有多行效果的运行时快照。
 *
 * 这个快照不是持久化模型，而是 `battle-rules` 适配层交给 `battle-engine` 前的中间装配结果。字段顺序尽量贴近
 * `BattleSkillSlot` 构造参数，方便审阅“数据库子表 -> 引擎技能槽”的映射是否完整。
 */
data class BattleSkillRuleEffectRuntimeSnapshot(
	val chargeSkippedByWeathers: Set<BattleWeather>,
	val accuracyOverridesByWeather: Map<BattleWeather, Int?>,
	val powerMultipliersByWeather: Map<BattleWeather, Double>,
	val elementOverridesByWeather: Map<BattleWeather, Long>,
	val statusApplications: List<BattleStatusApplication>,
	val volatileStatusApplications: List<BattleVolatileStatusApplication>,
	val statStageEffects: List<BattleStatStageEffect>,
	val statStageOperations: List<BattleStatStageOperation>,
	val sideConditionApplications: List<BattleSideConditionApplication>,
	val sideSpeedModifierApplications: List<BattleSideSpeedModifierApplication>,
	val sideEntryHazardApplications: List<BattleSideEntryHazardApplication>,
	val fieldSpeedOrderApplications: List<BattleFieldSpeedOrderApplication>,
) {
	companion object {
		val EMPTY = BattleSkillRuleEffectRuntimeSnapshot(
			chargeSkippedByWeathers = emptySet(),
			accuracyOverridesByWeather = emptyMap(),
			powerMultipliersByWeather = emptyMap(),
			elementOverridesByWeather = emptyMap(),
			statusApplications = emptyList(),
			volatileStatusApplications = emptyList(),
			statStageEffects = emptyList(),
			statStageOperations = emptyList(),
			sideConditionApplications = emptyList(),
			sideSpeedModifierApplications = emptyList(),
			sideEntryHazardApplications = emptyList(),
			fieldSpeedOrderApplications = emptyList(),
		)
	}
}
