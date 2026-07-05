package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderApplication
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderEffect
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleSideConditionApplication
import io.github.lishangbu.battleengine.model.BattleSideConditionTarget
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
import io.github.lishangbu.battleengine.model.BattleSideEntryHazard
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardApplication
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifier
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierApplication
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatStageOperation
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.common.web.invalidValue
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
	fun ruleEffects(ruleId: Long): BattleSkillRuleEffectRuntimeSnapshot {
		val statusEffects = statusEffectRows(ruleId)
		val sideFieldEffects = sideFieldEffectRows(ruleId)
		validateStatusEffectKinds(statusEffects)
		validateSideFieldEffectPolicies(sideFieldEffects)
		return BattleSkillRuleEffectRuntimeSnapshot(
			chargeSkippedByWeathers = chargeSkippedByWeathers(ruleId),
			accuracyOverridesByWeather = weatherAccuracyOverrides(ruleId),
			powerMultipliersByWeather = weatherPowerMultipliers(ruleId),
			groundedPowerMultipliersByTerrain = groundedTerrainPowerMultipliers(ruleId),
			elementOverridesByWeather = weatherElementOverrides(ruleId),
			elementOverridesByTerrain = terrainElementOverrides(ruleId),
			statusApplications = statusApplications(statusEffects),
			volatileStatusApplications = volatileStatusApplications(statusEffects),
			statStageEffects = statStageEffects(ruleId),
			statStageOperations = statStageOperations(ruleId),
			sideConditionApplications = sideConditionApplications(sideFieldEffects),
			sideSpeedModifierApplications = sideSpeedModifierApplications(sideFieldEffects),
			sideEntryHazardApplications = sideEntryHazardApplications(sideFieldEffects),
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

	private fun groundedTerrainPowerMultipliers(ruleId: Long): Map<BattleTerrain, Double> =
		jdbcTemplate.query(
			"""
			select t.code as terrain_code, m.power_multiplier
			from battle_skill_terrain_power_modifier m
			join battle_terrain_rule t on t.id = m.terrain_rule_id
			where m.skill_rule_id = ? and m.enabled = true and t.enabled = true
			order by m.sort_order, m.id
			""".trimIndent(),
			{ rs, _ -> rs.getString("terrain_code").toBattleTerrain() to rs.getDouble("power_multiplier") },
			ruleId,
		).toMap()

	private fun terrainElementOverrides(ruleId: Long): Map<BattleTerrain, Long> =
		jdbcTemplate.query(
			"""
			select t.code as terrain_code, o.target_element_id
			from battle_skill_terrain_element_override o
			join battle_terrain_rule t on t.id = o.terrain_rule_id
			join game_element e on e.id = o.target_element_id
			where o.skill_rule_id = ? and o.enabled = true and t.enabled = true and e.enabled = true
			order by o.sort_order, o.id
			""".trimIndent(),
			{ rs, _ -> rs.getString("terrain_code").toBattleTerrain() to rs.getLong("target_element_id") },
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

	/**
	 * 一次读取技能附加状态效果，再按状态族拆成主要异常和临时状态。
	 *
	 * 数据库中主要异常和临时状态共用 `battle_skill_status_effect`，真正区分来自 `battle_status_rule.status_kind`。
	 * 先把共用行读成 [StatusEffectRuntimeRow]，可以保证目标作用域、概率和排序只解析一次；后续如果状态效果新增
	 * 持续回合或触发时机字段，也只需要在这一个查询里补列。目标作用域必须显式可识别，不能返回 null 后被
	 * `filterNotNull` 吞掉；状态附加行一旦被静默丢弃，实战里会表现成技能命中但异常状态永远不会触发。
	 */
	private fun statusEffectRows(ruleId: Long): List<StatusEffectRuntimeRow> =
		jdbcTemplate.query(
			"""
			select sr.code as status_code, sr.status_kind, e.target_scope, e.chance_percent
			from battle_skill_status_effect e
			join battle_status_rule sr on sr.id = e.status_rule_id
			where e.skill_rule_id = ?
				and e.enabled = true
				and sr.enabled = true
				and e.effect_timing = 'AFTER_HIT'
			order by e.sort_order, e.id
			""".trimIndent(),
			{ rs, _ ->
				StatusEffectRuntimeRow(
					statusCode = rs.getString("status_code"),
					statusKind = rs.getString("status_kind"),
					target = rs.battleEffectTarget("target_scope", "状态效果"),
					chancePercent = rs.getInt("chance_percent"),
				)
			},
			ruleId,
		)

	private fun statusApplications(rows: List<StatusEffectRuntimeRow>): List<BattleStatusApplication> =
		rows.filter { it.statusKind == "MAJOR" }
			.map { row ->
				BattleStatusApplication(
					status = row.statusCode.toBattleMajorStatus(),
					target = row.target,
					chancePercent = row.chancePercent,
				)
			}

	private fun volatileStatusApplications(rows: List<StatusEffectRuntimeRow>): List<BattleVolatileStatusApplication> =
		rows.filter { it.statusKind == "VOLATILE" }
			.map { row ->
				BattleVolatileStatusApplication(
					status = row.statusCode.toBattleVolatileStatus(),
					target = row.target,
					chancePercent = row.chancePercent,
				)
			}

	/**
	 * 校验状态效果族是否能被当前运行时拆分。
	 *
	 * 状态附加子表先读取成 [StatusEffectRuntimeRow]，再按 `statusKind` 拆成主要异常和临时状态两份列表。如果这里不先
	 * 校验，未知状态族会同时避开两个 `filter`，最终表现成启用的状态规则完全没有进入技能槽。生产环境中这比直接
	 * 报错更危险，因为技能表面上仍能装配成功，只有实战时才发现状态不会触发。
	 */
	private fun validateStatusEffectKinds(rows: List<StatusEffectRuntimeRow>) {
		val unsupported = rows.firstOrNull { row -> row.statusKind != "MAJOR" && row.statusKind != "VOLATILE" }
		if (unsupported != null) {
			invalidValue("statusKind", "不支持的状态效果类型: ${unsupported.statusKind}")
		}
	}

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
				BattleStatStageEffect(
					stat = rs.getString("stat_code").toBattleStat(),
					target = rs.battleEffectTarget("target_scope", "能力阶级效果"),
					stageDelta = rs.getInt("stage_delta"),
					chancePercent = rs.getInt("chance_percent"),
				)
			},
			ruleId,
		)

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

	/**
	 * 一次读取一侧场地效果，再按 effect_policy 映射成屏障、速度修正或入场陷阱。
	 *
	 * `battle_skill_field_effect` 同时承载三类运行时效果；它们的过滤条件、排序条件和天气前置条件完全一致，差异只在
	 * `battle_field_rule.effect_policy` 最终能映射成哪种引擎模型。保留三段几乎相同的 SQL 会让新增字段时出现三处
	 * 同步点，也会让每个技能规则多做两次无意义查询。因此这里先冻结成行对象，再用三个窄映射函数拆回
	 * `BattleSkillSlot` 需要的列表。
	 */
	private fun sideFieldEffectRows(ruleId: Long): List<SideFieldEffectRuntimeRow> =
		jdbcTemplate.query(
			"""
			select
				fr.effect_policy as field_effect_policy,
				fr.min_turns,
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
				SideFieldEffectRuntimeRow(
					effectPolicy = rs.getString("field_effect_policy"),
					minTurns = rs.nullableInt("min_turns"),
					maxLayers = rs.nullableInt("max_layers"),
					targetSide = rs.getString("target_side").toBattleSideConditionTarget(),
					chancePercent = rs.getInt("chance_percent"),
					requiredWeather = rs.getString("required_weather_code")?.toBattleWeather(),
				)
			},
			ruleId,
		)

	private fun sideConditionApplications(rows: List<SideFieldEffectRuntimeRow>): List<BattleSideConditionApplication> =
		rows.mapNotNull { row ->
			val reductionKind = row.effectPolicy.toBattleSideDamageReductionKind() ?: return@mapNotNull null
			BattleSideConditionApplication(
				targetSide = row.targetSide,
				damageReduction = BattleSideDamageReduction(
					kind = reductionKind,
					turnsRemaining = row.minTurns,
				),
				chancePercent = row.chancePercent,
				requiredWeather = row.requiredWeather,
			)
		}

	private fun sideSpeedModifierApplications(rows: List<SideFieldEffectRuntimeRow>): List<BattleSideSpeedModifierApplication> =
		rows.mapNotNull { row ->
			val modifierKind = row.effectPolicy.toBattleSideSpeedModifierKind() ?: return@mapNotNull null
			BattleSideSpeedModifierApplication(
				targetSide = row.targetSide,
				speedModifier = BattleSideSpeedModifier(
					kind = modifierKind,
					turnsRemaining = row.minTurns,
				),
				chancePercent = row.chancePercent,
				requiredWeather = row.requiredWeather,
			)
		}

	private fun sideEntryHazardApplications(rows: List<SideFieldEffectRuntimeRow>): List<BattleSideEntryHazardApplication> =
		rows.mapNotNull { row ->
			val hazardKind = row.effectPolicy.toBattleSideEntryHazardKind() ?: return@mapNotNull null
			BattleSideEntryHazardApplication(
				targetSide = row.targetSide,
				hazard = BattleSideEntryHazard(
					kind = hazardKind,
					maxLayers = row.maxLayers ?: hazardKind.defaultMaxLayers,
				),
				chancePercent = row.chancePercent,
				requiredWeather = row.requiredWeather,
			)
		}

	/**
	 * 校验一侧场地效果 policy 至少能被某一种一侧效果模型承载。
	 *
	 * 同一批 `battle_skill_field_effect` 行会被拆成屏障、速度修正和入场陷阱三个列表，所以单个列表里的 `mapNotNull`
	 * 不能把未命中的 policy 直接视为错误：顺风行对于屏障列表本来就应该返回 null。这里在拆分前先做一次总分类，
	 * 只有当一个 policy 既不是屏障、也不是速度修正、也不是入场陷阱时才失败。这样保留三类列表的简单实现，
	 * 同时避免启用中的资料因为拼写错误或 mapper 漏补，被生产运行时静默装配成“技能没有任何一侧场地效果”。
	 */
	private fun validateSideFieldEffectPolicies(rows: List<SideFieldEffectRuntimeRow>) {
		val unsupported = rows.firstOrNull { row ->
			row.effectPolicy.toBattleSideDamageReductionKind() == null &&
				row.effectPolicy.toBattleSideSpeedModifierKind() == null &&
				row.effectPolicy.toBattleSideEntryHazardKind() == null
		}
		if (unsupported != null) {
			invalidValue("effectPolicy", "不支持的一侧场地效果策略: ${unsupported.effectPolicy}")
		}
	}

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
				val effectPolicy = rs.getString("field_effect_policy")
				val speedOrderKind = effectPolicy.toBattleFieldSpeedOrderKind()
					?: invalidValue("effectPolicy", "不支持的全场速度顺序效果策略: $effectPolicy")
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
		)

	/**
	 * 解析技能效果子表中的目标作用域。
	 *
	 * `BattleEffectTarget` 只允许“使用者”和“当前实际目标”两种运行时语义；范围技能已经在引擎命中阶段逐目标展开，
	 * 因此资料中的 ALL_OPPONENTS 会在 mapper 中压成 TARGET。除这几种已知值以外，任何目标作用域都代表资料
	 * 或后台写入错误，必须立即失败。这里不让调用方拿到 null，是为了防止查询结果被 `filterNotNull` 继续静默丢弃。
	 */
	private fun ResultSet.battleEffectTarget(column: String, context: String): BattleEffectTarget {
		val targetScope = getString(column)
		return targetScope.toBattleEffectTarget()
			?: invalidValue("targetScope", "不支持的${context}目标作用域: $targetScope")
	}

	private fun ResultSet.nullableInt(column: String): Int? {
		val value = getInt(column)
		return if (wasNull()) null else value
	}
}

/**
 * `battle_skill_status_effect` 和 `battle_status_rule` 合并后的最小运行时行。
 *
 * 同一张技能状态效果表既能表达主要异常，也能表达混乱、畏缩、回复封锁等临时状态；`statusKind` 是拆分两类引擎
 * 模型的唯一依据。这里先保存已经转换过的 [target]，避免两个列表各自重新解释 `target_scope` 时出现不一致。
 */
private data class StatusEffectRuntimeRow(
	val statusCode: String,
	val statusKind: String,
	val target: BattleEffectTarget,
	val chancePercent: Int,
)

/**
 * `battle_skill_field_effect` 和 `battle_field_rule` 合并后的最小运行时行。
 *
 * 这不是对数据库表的通用复刻，只保留一侧场地效果映射三种引擎模型时都会用到的列。`effectPolicy` 决定该行最终
 * 被屏障、速度修正或入场陷阱列表消费；`minTurns` 只对持续回合类效果有意义，`maxLayers` 只对入场陷阱有意义。
 * 把这些字段先冻结成强类型行，可以避免三段 SQL 各自解析天气、作用侧和概率时出现细小分歧。
 */
private data class SideFieldEffectRuntimeRow(
	val effectPolicy: String,
	val minTurns: Int?,
	val maxLayers: Int?,
	val targetSide: BattleSideConditionTarget,
	val chancePercent: Int,
	val requiredWeather: BattleWeather?,
)

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
	val groundedPowerMultipliersByTerrain: Map<BattleTerrain, Double>,
	val elementOverridesByWeather: Map<BattleWeather, Long>,
	val elementOverridesByTerrain: Map<BattleTerrain, Long>,
	val statusApplications: List<BattleStatusApplication>,
	val volatileStatusApplications: List<BattleVolatileStatusApplication>,
	val statStageEffects: List<BattleStatStageEffect>,
	val statStageOperations: List<BattleStatStageOperation>,
	val sideConditionApplications: List<BattleSideConditionApplication>,
	val sideSpeedModifierApplications: List<BattleSideSpeedModifierApplication>,
	val sideEntryHazardApplications: List<BattleSideEntryHazardApplication>,
	val fieldSpeedOrderApplications: List<BattleFieldSpeedOrderApplication>,
)
