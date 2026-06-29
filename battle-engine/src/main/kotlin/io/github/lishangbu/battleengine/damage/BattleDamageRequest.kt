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
) {
	init {
		require(randomPercent in 85..100) { "randomPercent must be between 85 and 100" }
		require(targetMultiplier > 0.0) { "targetMultiplier must be positive" }
		require(sideDamageReductionMultiplier > 0.0) { "sideDamageReductionMultiplier must be positive" }
	}
}
