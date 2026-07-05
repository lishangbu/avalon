package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState

/**
 * 命中后清理全场入口陷阱与替身的技能效果。
 *
 * 大扫除类现代规则和高速旋转类规则的关键差异在作用域：它不只清理使用者一侧，而是移除双方场地上的所有入口
 * 陷阱，并直接移除当前场上所有替身；同时它仍然允许自身攻速提升在没有任何可清目标时正常发生。把这条规则独立
 * 于 [BattleUserSideCleanupEffects]，可以避免“己方清场”和“全场整理”共享一个含糊布尔值，后续维护数据时也能
 * 一眼看到不同 policy 对应的真实战斗含义。
 *
 * 本类只在 [BattleSkillAdditionalEffects] 的成功后流水线中调用，因此保护、未命中、属性免疫或行动前失败已经在
 * 更早阶段处理完毕。这里不消费随机数，也不产生技能失败事件；清场技能没有可清状态时仍然是一次成功行动。
 */
internal class BattleFieldCleanupEffects {
	/**
	 * 应用技能声明的全场清理效果。
	 *
	 * 顺序固定为先移除双方一侧入口陷阱，再移除当前所有上场成员的替身。这样事件流先描述场地状态变化，再描述
	 * 成员身上替身变化；两类状态互不依赖，所以不需要为不同陷阱或不同替身引入额外优先级。
	 */
	fun apply(state: BattleState, actorId: String, skill: BattleSkillSlot): BattleState {
		if (!skill.clearsFieldHazardsAndSubstitutes) {
			return state
		}
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle()) {
			return state
		}
		val afterEntryHazards = clearAllSideEntryHazards(state, actorId, skill)
		return clearActiveSubstitutes(afterEntryHazards, skill)
	}

	/**
	 * 移除双方所有入口陷阱。
	 *
	 * [BattleState.clearSideEntryHazards] 会一次性清空某一侧，并返回移除前真实存在的陷阱快照；这里只负责遍历双方
	 * side，并把每个被删除的陷阱转换成可复盘事件。多层陷阱按 kind 产生一条移除事件，层数细节保留在状态变更
	 * 快照中供测试断言，前端战斗日志不需要把“第几层”重复展开。
	 */
	private fun clearAllSideEntryHazards(state: BattleState, actorId: String, skill: BattleSkillSlot): BattleState =
		state.sides.fold(state) { current, sideSnapshot ->
			val removal = current.clearSideEntryHazards(sideSnapshot.sideId) ?: return@fold current
			removal.state.appendEvents(
				removal.removedHazards.map { hazard ->
					BattleEvent.SideEntryHazardRemoved(
						turnNumber = current.turnNumber,
						actorId = actorId,
						sideId = sideSnapshot.sideId,
						kind = hazard.kind,
						skillId = skill.skillId,
					)
				},
			)
		}

	/**
	 * 移除当前场上所有替身。
	 *
	 * 替身属于在场状态，离场时已经由 [leaveBattlefield] 清理；因此这里只遍历当前上场席位，避免错误清掉后备成员
	 * 理论上不应存在的替身残留。若某个上场成员没有替身，保持状态和事件流不变。
	 */
	private fun clearActiveSubstitutes(state: BattleState, skill: BattleSkillSlot): BattleState =
		state.sides
			.flatMap { it.activeActorIds }
			.fold(state) { current, activeActorId ->
				val participant = current.participant(activeActorId) ?: return@fold current
				if (!participant.hasSubstitute()) {
					current
				} else {
					current
						.replaceParticipant(participant.clearSubstitute())
						.appendEvent(
							BattleEvent.SubstituteCleared(
								turnNumber = current.turnNumber,
								actorId = activeActorId,
								skillId = skill.skillId,
							),
						)
				}
			}
}
