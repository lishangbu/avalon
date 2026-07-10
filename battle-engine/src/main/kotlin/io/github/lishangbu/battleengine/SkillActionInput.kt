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
 * 技能行动排序前的候选输入。
 *
 * [action] 表达本回合要尝试使用哪一格技能以及指向哪个目标槽位；[source] 标记它来自玩家提交、锁招续回合、
 * 蓄力释放还是无技能可用时的挣扎 fallback。只有挣扎这类运行时内置技能会通过 [skillOverride] 携带临时技能快照；
 * 普通资料技能必须继续从行动者当前技能槽读取，才能正确校验 PP、讲究锁定和后续状态变化。该类型不表示“已经能执行”，
 * 它只是排序器和执行器之间的窄接口。
 */
internal data class SkillActionInput(
	val action: BattleAction.UseSkill,
	val source: SkillActionSource,
	val skillOverride: BattleSkillSlot? = null,
)
