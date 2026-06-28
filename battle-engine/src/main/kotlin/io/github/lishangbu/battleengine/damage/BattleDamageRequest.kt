package io.github.lishangbu.battleengine.damage

import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSkillSlot

/**
 * 一次普通伤害计算输入。
 *
 * 输入同时包含攻击方、目标、技能槽、规则快照、伤害随机百分比、目标范围倍率和本次是否击中要害。
 * 命中、击中要害和伤害浮动等随机数在进入计算器之前已经被消费，因此计算器本身是纯函数，
 * 便于公式级测试直接覆盖取整、范围目标修正、属性一致加成、属性克制和击中要害倍率。
 */
data class BattleDamageRequest(
	val attacker: BattleParticipant,
	val defender: BattleParticipant,
	val skill: BattleSkillSlot,
	val rules: BattleRuleSnapshot,
	val environment: BattleEnvironment = BattleEnvironment(),
	val randomPercent: Int,
	val targetMultiplier: Double = 1.0,
	val criticalHit: Boolean = false,
) {
	init {
		require(randomPercent in 85..100) { "randomPercent must be between 85 and 100" }
		require(targetMultiplier > 0.0) { "targetMultiplier must be positive" }
	}
}
