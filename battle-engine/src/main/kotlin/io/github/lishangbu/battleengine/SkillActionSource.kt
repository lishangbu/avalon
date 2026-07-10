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
 * 技能行动候选的来源。
 *
 * 来源会影响执行阶段的副作用：玩家提交行动需要正常消耗 PP 并受讲究锁定校验；锁招续回合和蓄力释放来自运行态，
 * 不应被新的玩家选择覆盖；挣扎 fallback 使用运行时内置技能，不回写资料技能槽。把来源做成枚举而不是布尔组合，
 * 可以让后续维护者在新增来源时显式处理所有分支，避免“是否消耗 PP”“是否解除蓄力”“是否可被玩家覆盖”等规则被
 * 隐含在多个可空字段里。
 */
internal enum class SkillActionSource {
	SUBMITTED,
	LOCKED_CONTINUATION,
	CHARGED_RELEASE,
	STRUGGLE_FALLBACK,
}
