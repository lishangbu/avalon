package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.makesEffectiveContact
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 命中前 gate 的结算结果。
 *
 * [Interrupted] 表示技能已经被属性、场地、特性、保护或命中失败阻止，调用方必须追加事件并结束后续流程。
 * [Passed] 表示技能可以继续进入属性吸收、状态效果或伤害阶段，同时携带已计算好的防守特性忽略事实。
 */
internal sealed interface BattlePreHitTargetGateResult {
	/**
	 * 命中前 gate 已经终止本次目标结算。
	 *
	 * [event] 是可以直接追加到战斗事件流的事实，可能表示招式未命中、属性免疫、场地阻挡、保护成功、特性吸收、
	 * 一击必杀等级失败等“技能已经走到逐目标阶段，但不能继续影响该目标”的结果。调用方收到该分支后必须停止
	 * 对该目标的状态、伤害和接触副作用结算；锁招、蓄力或连续行动的中断清理由外层阶段继续处理。
	 */
	data class Interrupted(val event: BattleEvent) : BattlePreHitTargetGateResult

	/**
	 * 命中前 gate 允许技能继续影响目标。
	 *
	 * [ignoresTargetAbilityEffects] 冻结本次逐目标结算是否无视目标侧防守特性。这个事实在命中前已经由技能规则
	 * 判定完成，后续伤害公式、状态阻挡和接触反制都应读取同一布尔值，而不是重新解释技能资料，避免同一次命中
	 * 中不同阶段对“是否无视特性”得出不一致结论。
	 */
	data class Passed(val ignoresTargetAbilityEffects: Boolean) : BattlePreHitTargetGateResult
}
