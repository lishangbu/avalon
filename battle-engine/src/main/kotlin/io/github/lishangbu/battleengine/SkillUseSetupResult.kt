package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 技能前置阶段的公开返回值。
 *
 * 该类型只在 `battleengine` 包内部流转，避免把准备阶段拆分结果编码成多个可空字段。调用方通过 sealed 分支可以
 * 明确区分“本次行动已经结束”和“可以继续保护/逐目标结算”，也避免遗漏蓄力开始这种不是失败、但本回合不命中的情况。
 */
internal sealed interface SkillUseSetupResult {
	/**
	 * 技能行动在前置阶段已经结束。
	 *
	 * [context] 包含前置阶段追加的事件和状态修改，例如行动前状态阻止、畏缩、麻痹不能行动、蓄力开始、休整跳过、
	 * 锁招中断或挣扎替换后的失败。调用方收到该分支后不能再执行保护、命中、伤害或附加效果，否则会把已经被
	 * 现代规则终止的行动重复推进到后续阶段。
	 */
	data class Stopped(val context: TurnContext) : SkillUseSetupResult

	/**
	 * 技能已经通过行动前检查，可以进入逐目标命中与效果结算。
	 *
	 * 该快照一次性冻结前置阶段产生的关键事实：[beforeMoveContext] 保存行动前状态和事件，
	 * [stateAfterChargeDecision] 表示蓄力/跳过蓄力等规则处理后的状态，[readyActor] 是可行动成员的最新快照，
	 * [actorAfterActionSetup] 是消耗 PP、写入锁招或讲究锁定后的使用者快照，[skill] 是本次实际使用的技能槽，
	 * [targets] 是当前阶段解析出的候选目标列表，[targetMultiplier] 是范围目标伤害倍率。拆成显式结果可以让
	 * 后续 resolver 不必重新读取旧状态推断这些中间事实，也降低阶段顺序被维护者无意改乱的风险。
	 */
	data class Ready(
		val beforeMoveContext: TurnContext,
		val stateAfterChargeDecision: BattleState,
		val readyActor: BattleParticipant,
		val actorAfterActionSetup: BattleParticipant,
		val skill: BattleSkillSlot,
		val targets: List<BattleParticipant>,
		val targetMultiplier: Double,
	) : SkillUseSetupResult
}
