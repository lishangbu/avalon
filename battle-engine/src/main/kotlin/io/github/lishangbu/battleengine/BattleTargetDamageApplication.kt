package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFatalDamageSurvivalSource
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 目标本体伤害写入结果。
 *
 * `state` 是已经写入 HP、追加伤害事件和保命事件后的状态；`damagedTarget` 是这份状态中的最新目标快照；
 * `actualDamageAmount` 是真实扣掉的 HP。调用方必须使用这个实际伤害量继续结算吸取、反伤、伤害后回复和倒下判定，
 * 不能回头使用公式给出的原始伤害值。
 */
internal data class BattleTargetDamageApplication(
	val state: BattleState,
	val damagedTarget: BattleParticipant,
	val actualDamageAmount: Int,
)
