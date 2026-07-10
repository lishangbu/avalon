package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageModifiers
import io.github.lishangbu.battleengine.model.BattleState
import kotlin.math.floor

/**
 * 单次技能行动的优先度上下文。
 *
 * `effectivePriority` 作为行动排序第一键；后两个字段记录变化技能优先度提升带来的后续规则影响，让精神场地、
 * 先制阻挡和目标属性免疫读取同一份结果，避免在不同阶段重复推导。
 */
internal data class SkillPriorityContext(
	val effectivePriority: Int,
	val statusPriorityBoostedByAbility: Boolean = false,
	val darkElementTargetsImmune: Boolean = false,
)
