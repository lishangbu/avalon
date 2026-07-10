package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import kotlin.math.floor

/**
 * 一段可被反打技能读取的受伤记录。
 *
 * `source` 是仍在场且可战斗的伤害来源；`amount` 是按技能规则倍数换算后的返还伤害。这里保存的是已经计算好的
 * 直接伤害数值，而不是原始事件，避免调用方重复理解分子分母和向下取整规则。
 */
internal data class BattleReceivedDamageHit(
	val source: BattleParticipant,
	val amount: Int,
)
