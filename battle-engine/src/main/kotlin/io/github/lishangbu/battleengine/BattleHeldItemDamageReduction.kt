package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFatalDamageSurvivalSource
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState

/**
 * 属性减伤携带道具触发后的目标快照与事件。
 *
 * 伤害计算器已经把倍率用于最终伤害数值；这里返回的是为了同步“是否消费道具”和对应事件，确保目标快照与事件
 * 不会因为普通伤害路径和未来其它伤害路径分叉。
 */
internal data class BattleHeldItemDamageReduction(
	val target: BattleParticipant,
	val event: BattleEvent.DamageReducedByItem,
)
