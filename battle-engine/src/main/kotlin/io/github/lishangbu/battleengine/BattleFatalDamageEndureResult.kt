package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFatalDamageSurvivalSource
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 挺住姿态处理后的伤害中间结果。
 *
 * [target] 当前不会被修改，但保留字段是为了和满 HP 保命结果保持一致；如果后续现代规则出现“挺住触发后清理某个
 * 一次性状态”的情况，调用方无需再改返回形状。[damageAmount] 是实际要写入 HP 的伤害，[event] 只在挺住真正
 * 抵消致命技能伤害时存在。
 */
internal data class BattleFatalDamageEndureResult(
	val target: BattleParticipant,
	val damageAmount: Int,
	val event: BattleEvent.FatalDamageSurvived? = null,
)
