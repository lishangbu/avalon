package io.github.lishangbu.battleengine.damage

import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSkillSlot

/**
 * 一次普通伤害计算输入。
 *
 * 输入同时包含攻击方、目标、技能槽、规则快照、伤害随机百分比、目标范围倍率、防守方侧屏障倍率和本次是否击中要害。
 * 命中、击中要害和伤害浮动等随机数在进入计算器之前已经被消费，因此计算器本身是纯函数，
 * 便于公式级测试直接覆盖取整、范围目标修正、属性一致加成、属性克制和击中要害倍率。
 * `ignoreDefenderAbilityEffects` 表示外层技能流程已经确认本次技能会无视目标侧防守特性，计算器只据此跳过
 * 目标作为防守方提供的公式特性，不参与判断具体特性名称或目标阵营。
 * `allowDefenderItemDamageReduction` 由状态机在替身等前置防护判定后传入；如果本体不会直接承受该次技能伤害，
 * 防守方一次性减伤道具不参与公式，也不会被消费。
 * `attackerEffectiveSpeed` 和 `defenderEffectiveSpeed` 只服务于电球、陀螺球这类按有效速度差推导基础威力的技能；
 * 由外层状态机使用行动排序同一套速度公式计算后传入，避免伤害计算器重新实现天气、场地、道具和顺风等速度规则。
 * [skillElementId] 在请求创建时冻结本次技能的有效属性，包含天气球、场地脉冲等天气/场地属性覆盖；非无属性
 * 伤害的公式、特性倍率、道具倍率和属性克制都读取同一个值，避免同一次伤害结算中多个调用点各自重新解释环境覆盖。
 * [typeEffectiveness] 则把“无属性伤害固定中性倍率”这条例外也冻结在请求上，避免公式、特性和道具各自重复判断。
 */
data class BattleDamageRequest(
	val attacker: BattleParticipant,
	val defender: BattleParticipant,
	val skill: BattleSkillSlot,
	val rules: BattleRuleSnapshot,
	val environment: BattleEnvironment = BattleEnvironment(),
	val randomPercent: Int,
	val targetMultiplier: Double = 1.0,
	val sideDamageReductionMultiplier: Double = 1.0,
	val criticalHit: Boolean = false,
	val ignoreDefenderAbilityEffects: Boolean = false,
	val allowDefenderItemDamageReduction: Boolean = true,
	val attackerEffectiveSpeed: Int? = null,
	val defenderEffectiveSpeed: Int? = null,
) {
	init {
		require(randomPercent in 85..100) { "randomPercent must be between 85 and 100" }
		require(targetMultiplier > 0.0) { "targetMultiplier must be positive" }
		require(sideDamageReductionMultiplier > 0.0) { "sideDamageReductionMultiplier must be positive" }
		require(attackerEffectiveSpeed == null || attackerEffectiveSpeed > 0) {
			"attackerEffectiveSpeed must be positive when present"
		}
		require(defenderEffectiveSpeed == null || defenderEffectiveSpeed > 0) {
			"defenderEffectiveSpeed must be positive when present"
		}
	}

	/**
	 * 本次普通伤害公式使用的技能有效属性。
	 *
	 * 这是一个派生快照，不参与数据类构造、相等性或复制参数；调用方只需要继续传入技能和当前环境。把它放在请求
	 * 对象中，是为了保证一次公式计算内的属性一致加成、属性克制、天气/场地倍率、特性与道具倍率都共享同一个
	 * 天气/场地覆盖结果，后续新增属性覆盖来源时也只需要维护 [BattleSkillSlot.effectiveElementId]。
	 */
	val skillElementId: Long = skill.effectiveElementId(environment.weather, environment.terrain)

	/**
	 * 本次普通伤害公式使用的属性克制倍率。
	 *
	 * 普通技能从规则快照读取属性相性；无属性伤害固定视为 1.0。把该值作为请求派生属性，可以让伤害公式、防守方
	 * 效果绝佳减伤、攻击方效果绝佳增伤等读取同一口径，避免某个分支忘记现代挣扎这类无属性技能不参与属性相性。
	 */
	val typeEffectiveness: Double =
		if (skill.typelessDamage) 1.0 else rules.elementChart.multiplier(skillElementId, defender.elementIds)
}
