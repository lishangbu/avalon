package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 已经通过存在性校验并带有排序上下文的技能行动计划。
 *
 * [actor] 和 [skill] 是排序阶段读取到的快照，用于计算优先度、速度和同速随机顺序；真正结算技能前，状态机仍会在
 * 关键 gate 处重新读取当前 [BattleState]，以处理替换、倒下、束缚解除、蓄力中断等前序行动造成的变化。
 * [priorityContext] 把先制度提升、先制免疫标记和有效优先度固定在同一份值对象里，防止后续多个阶段各自重新计算后
 * 对同一行动得出不同结论。
 */
internal data class ActionPlan(
	val action: BattleAction.UseSkill,
	val actor: BattleParticipant,
	val skill: BattleSkillSlot,
	val source: SkillActionSource,
	val priorityContext: SkillPriorityContext,
)
