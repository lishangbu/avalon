package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.SkillPreventionReason
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 行动前状态阻止的返回值。
 *
 * [state] 是处理行动前状态后的最新战斗状态；[blocked] 表示本次技能行动是否已经被完全阻止。这个结果类型不携带
 * [BattleEngine] 的 `TurnContext`，因为行动前阶段只会推进战斗状态，不会修改本回合保护集合等临时编排字段。
 */
internal data class BattleBeforeMoveResult(
	val state: BattleState,
	val blocked: Boolean,
) {
	internal companion object {
		/**
		 * 构造“行动前阶段已经完全阻止本次技能”的结果。
		 *
		 * 使用命名工厂而不是裸 `blocked = true`，是为了让 resolver 主流程读起来像规则说明：每个分支只表达
		 * “该状态阻止了行动”，具体状态递减、事件追加和随机消费仍保留在对应私有函数中。
		 */
		fun blocked(state: BattleState): BattleBeforeMoveResult =
			BattleBeforeMoveResult(state = state, blocked = true)

		/**
		 * 构造“行动前阶段已经处理完成，但本次技能仍继续执行”的结果。
		 *
		 * 冰冻解除和混乱自然结束都会修改状态并继续行动；这个工厂避免它们和完全无状态变化的继续分支在调用处
		 * 混成相同的裸布尔值。
		 */
		fun continues(state: BattleState): BattleBeforeMoveResult =
			BattleBeforeMoveResult(state = state, blocked = false)
	}
}
