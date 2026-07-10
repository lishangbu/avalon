package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSideProtectionKind
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 单目标结算开始前的目标解析结果。
 *
 * 普通目标缺失时保持既有“跳过该目标”的行为；已受伤害反打技能找不到合格伤害记忆时需要追加技能失败事件并返回
 * 新上下文。用显式结果类型比返回 nullable 目标更清楚，能避免后续维护时不小心吞掉失败事件。
 */
internal sealed interface BattleSkillTargetLookupResult {
	/**
	 * 逐目标结算找到了仍然有效的目标。
	 *
	 * [target] 是在当前 [TurnContext] 中重新读取到的最新成员快照，而不是行动提交时的旧对象。这样替换、倒下、
	 * 已受伤害记忆和随机目标重定向等前置阶段发生后，技能后续命中、伤害和状态效果始终使用同一份最新运行态。
	 */
	data class Resolved(val target: BattleParticipant) : BattleSkillTargetLookupResult

	/**
	 * 目标解析阶段已经产生失败事件并更新上下文。
	 *
	 * 该分支主要用于反打类技能缺少合格受伤记忆等“目标存在，但本次规则无法继续”的场景。返回 [context] 而非
	 * null，可以确保调用方保留已经追加的失败事件和锁招中断清理，不会因为把失败当成普通无目标而静默跳过。
	 */
	data class Stopped(val context: TurnContext) : BattleSkillTargetLookupResult
}
