package io.github.lishangbu.battleengine.damage

import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleSkillSlot

/**
 * 一次普通伤害计算输入。
 *
 * 输入同时包含攻击方、目标、技能槽、规则快照和伤害随机百分比。随机数在进入计算器之前已经被消费，
 * 因此计算器本身是纯函数，便于公式级测试直接覆盖取整、属性一致加成和属性克制。
 */
data class BattleDamageRequest(
	val attacker: BattleParticipant,
	val defender: BattleParticipant,
	val skill: BattleSkillSlot,
	val rules: BattleRuleSnapshot,
	val environment: BattleEnvironment = BattleEnvironment(),
	val randomPercent: Int,
) {
	init {
		require(randomPercent in 85..100) { "randomPercent must be between 85 and 100" }
	}
}
