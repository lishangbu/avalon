package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.common.web.invalidValue
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet

/**
 * 技能运行时资料读取器。
 *
 * 本类负责把 `game_skill` 与 `battle_skill_rule` 主行装配成 [BattleSkillSlot]：主技能行提供名称、属性、PP、
 * 威力、命中率和优先度；规则主行提供目标、命中、伤害和效果 policy，以及多段、蓄力、休整、接触等单列标记。
 * 天气修正、状态附加、能力阶级变化和场地效果这类一条规则下的多行明细，已经下沉到
 * [BattleSkillRuleEffectRuntimeLookup]。这样主行 policy 校验和子表 SQL 不再互相挤在一个类里，新增资料时更容易
 * 看清楚应该改“主规则装配”还是“规则明细装配”。
 *
 * 这里仍然返回 battle-engine 的强类型模型，而不是数据库行。原因是 SQL/JDBC 是 battle-rules 适配层细节，纯引擎
 * 不应知道规则资料拆成了哪些三范式表；它只消费已经冻结好的技能槽快照。
 */
@Component
class BattleSkillRuntimeLookup(
	private val jdbcTemplate: JdbcTemplate,
	private val ruleEffectLookup: BattleSkillRuleEffectRuntimeLookup,
) {
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
			r.hit_policy,
			r.damage_policy,
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

	row.requireSupportedRulePolicies()
	val ruleEffects = ruleEffectLookup.ruleEffects(row.ruleId)
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
		chargeSkippedByWeathers = ruleEffects.chargeSkippedByWeathers,
		rechargesAfterUse = row.rechargesAfterUse ?: false,
		accuracyOverridesByWeather = ruleEffects.accuracyOverridesByWeather,
		powerMultipliersByWeather = ruleEffects.powerMultipliersByWeather,
		elementOverridesByWeather = ruleEffects.elementOverridesByWeather,
		lockMoveTurnsMin = row.lockMoveTurnsMin ?: 1,
		lockMoveTurnsMax = row.lockMoveTurnsMax ?: 1,
		confusesUserAfterLock = row.confusesUserAfterLock ?: false,
		forceTargetSwitch = row.forceTargetSwitch ?: false,
		priority = row.priority,
		remainingPp = row.pp,
		maxPp = row.pp,
		statusApplications = ruleEffects.statusApplications,
		volatileStatusApplications = ruleEffects.volatileStatusApplications,
		statStageEffects = ruleEffects.statStageEffects,
		statStageOperations = ruleEffects.statStageOperations,
		sideConditionApplications = ruleEffects.sideConditionApplications,
		sideSpeedModifierApplications = ruleEffects.sideSpeedModifierApplications,
		sideEntryHazardApplications = ruleEffects.sideEntryHazardApplications,
		fieldSpeedOrderApplications = ruleEffects.fieldSpeedOrderApplications,
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
		hitPolicy = getString("hit_policy"),
		damagePolicy = getString("damage_policy"),
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

/**
 * 校验启用中的技能规则 policy 是否都已经被运行时显式支持。
 *
 * `game_skill` 可以没有对应的 `battle_skill_rule`，这种普通技能继续使用 battle-engine 的默认单体、普通命中、
 * 普通伤害模型；但一旦存在启用规则行，四个 policy 字段就不再允许依赖默认值。这样可以防止 Liquibase 或后台
 * 维护写入一个拼错的 `effect_policy`、`target_policy`、`hit_policy` 或 `damage_policy` 后，运行时仍然悄悄把
 * 它装配成“无附加效果/默认目标”的技能。
 */
private fun SkillRuntimeRow.requireSupportedRulePolicies() {
	if (ruleId == null) {
		return
	}
	val normalizedEffectPolicy = effectPolicy
		?: invalidValue("effectPolicy", "启用的技能规则缺少 effect_policy: skillId=$skillId")
	val normalizedTargetPolicy = targetPolicy
		?: invalidValue("targetPolicy", "启用的技能规则缺少 target_policy: skillId=$skillId")
	val normalizedHitPolicy = hitPolicy
		?: invalidValue("hitPolicy", "启用的技能规则缺少 hit_policy: skillId=$skillId")
	val normalizedDamagePolicy = damagePolicy
		?: invalidValue("damagePolicy", "启用的技能规则缺少 damage_policy: skillId=$skillId")
	if (!normalizedEffectPolicy.isBattleSkillRuntimeEffectPolicySupported()) {
		invalidValue("effectPolicy", "不支持的技能主效果策略: $normalizedEffectPolicy")
	}
	if (!normalizedTargetPolicy.isBattleSkillRuntimeTargetPolicySupported()) {
		invalidValue("targetPolicy", "不支持的技能目标策略: $normalizedTargetPolicy")
	}
	if (!normalizedHitPolicy.isBattleSkillRuntimeHitPolicySupported()) {
		invalidValue("hitPolicy", "不支持的技能命中策略: $normalizedHitPolicy")
	}
	if (!normalizedDamagePolicy.isBattleSkillRuntimeDamagePolicySupported()) {
		invalidValue("damagePolicy", "不支持的技能伤害策略: $normalizedDamagePolicy")
	}
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
	val hitPolicy: String?,
	val damagePolicy: String?,
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
