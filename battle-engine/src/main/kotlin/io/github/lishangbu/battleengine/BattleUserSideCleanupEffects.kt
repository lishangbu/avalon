package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleVolatileStatus

/**
 * 命中后清理使用者一侧持续限制的技能效果。
 *
 * 高速旋转、晶光转转的现代规则不是普通伤害公式的一部分，也不是目标身上的附加异常；它们在技能成功连接后，
 * 清理“使用者自己/使用者一侧”已经存在的持续限制：使用者身上的束缚、使用者身上的寄生种子，以及使用者一侧的
 * 入场陷阱。把这组规则放进独立 resolver，可以避免把入场陷阱、束缚和寄生种子的内部字段散落在伤害结算器里。
 *
 * 本类只在 [BattleSkillAdditionalEffects] 的成功后流水线中调用，因此不会处理未命中、保护阻挡、属性免疫或被
 * 吸收的情况。若使用者已经在伤害后的接触反制或反作用中倒下，则这里保持状态不变，避免已经离场的成员继续清场。
 */
internal class BattleUserSideCleanupEffects {
	/**
	 * 应用技能声明的使用者侧清理效果。
	 *
	 * 清理顺序固定为：先解除使用者身上的束缚，再解除寄生种子，最后清除使用者一侧全部入场陷阱。这个顺序让事件流
	 * 先记录成员自身状态变化，再记录场地状态变化；实际战斗结果上三者互不依赖，因此不需要额外随机数或复杂排序。
	 */
	fun apply(state: BattleState, actorId: String, skill: BattleSkillSlot): BattleState {
		if (!skill.clearsUserSideHazardsAndTraps) {
			return state
		}
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle()) {
			return state
		}
		val afterBinding = clearBinding(state, actorId)
		val afterLeechSeed = clearLeechSeed(afterBinding, actorId, skill)
		return clearUserSideEntryHazards(afterLeechSeed, actorId, skill)
	}

	/**
	 * 解除使用者身上的束缚类状态。
	 *
	 * 束缚在运行态中由来源 actorId 与剩余回合共同表达；只要任一字段存在，就通过 [clearBinding] 统一清理，避免
	 * 手动复制字段时遗漏来源或倒计时。事件沿用通用临时状态清除事件，因为束缚本身已经是 [BattleVolatileStatus]。
	 */
	private fun clearBinding(state: BattleState, actorId: String): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (actor.boundByActorId == null && actor.bindingTurnsRemaining == 0) {
			return state
		}
		return state
			.replaceParticipant(actor.clearBinding())
			.appendEvent(
				BattleEvent.VolatileStatusCleared(
					turnNumber = state.turnNumber,
					actorId = actorId,
					status = BattleVolatileStatus.BINDING,
				),
			)
	}

	/**
	 * 解除使用者身上的寄生种子。
	 *
	 * 寄生种子保存的是来源侧和来源上场席位，不属于普通临时状态枚举；因此它使用专门事件记录技能来源，方便 replay
	 * 和管理端事件列表解释“为什么后续回合末不再发生寄生扣血/回复”。
	 */
	private fun clearLeechSeed(state: BattleState, actorId: String, skill: BattleSkillSlot): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.isLeechSeeded()) {
			return state
		}
		return state
			.replaceParticipant(actor.clearLeechSeed())
			.appendEvent(
				BattleEvent.LeechSeedCleared(
					turnNumber = state.turnNumber,
					actorId = actorId,
					skillId = skill.skillId,
				),
			)
	}

	/**
	 * 清除使用者一侧的全部入场陷阱。
	 *
	 * 入场陷阱是挂在 side 上的状态，不属于使用者本人；清理时先通过 [BattleState.clearSideEntryHazards] 一次性写入
	 * 新 side，随后按移除前的陷阱顺序追加事件。多层陷阱会作为一个 kind 被整体移除，和公开规则“移除所有层数”
	 * 的语义一致。
	 */
	private fun clearUserSideEntryHazards(state: BattleState, actorId: String, skill: BattleSkillSlot): BattleState {
		val side = state.sideOf(actorId) ?: return state
		val removal = state.clearSideEntryHazards(side.sideId) ?: return state
		return removal.state.appendEvents(
			removal.removedHazards.map { hazard ->
				BattleEvent.SideEntryHazardRemoved(
					turnNumber = state.turnNumber,
					actorId = actorId,
					sideId = side.sideId,
					kind = hazard.kind,
					skillId = skill.skillId,
				)
			},
		)
	}
}
