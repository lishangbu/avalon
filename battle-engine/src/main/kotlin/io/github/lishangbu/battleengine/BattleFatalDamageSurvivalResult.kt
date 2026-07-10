package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFatalDamageSurvivalSource
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState

/**
 * 满 HP 保命规则的中间结果。
 *
 * [target] 可能是已经消耗携带道具后的目标快照；[damageAmount] 是本次真正要写入 HP 的伤害值；[event] 只在保命
 * 规则实际触发时存在。调用方会先写入伤害事件，再追加该事件，从而让 replay 清楚表达“受到了致命伤害，但被来源
 * 保住”的先后顺序。
 */
internal data class BattleFatalDamageSurvivalResult(
	val target: BattleParticipant,
	val damageAmount: Int,
	val event: BattleEvent.FatalDamageSurvived? = null,
)
