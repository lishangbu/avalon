package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSideProtectionKind
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 单个回合技能阶段的临时上下文。
 *
 * `state` 是不断推进的不可变战斗状态；`plannedSkillActions` 是行动排序阶段冻结的本回合技能计划；
 * `resolvedSkillActorIds` 保存本回合技能阶段已经处理过的行动者；`protectedActorIds` 保存本回合已经成功建立守住屏障的成员；
 * `multiTargetProtectedSideIds` 和 `priorityProtectedSideIds` 保存只在本回合有效的一侧临时防护；
 * `successfulProtectionActorIds` 保存回合结束后应保留连续保护类行动计数的成员，包括守住、看穿、挺住、广域防守和快速防守。
 * 这类回合内临时标记不进入 `BattleState`，避免被误认为跨回合持久状态，也方便后续扩展击掌奇袭、
 * 守住连续成功率、先制阻挡等同样只在当前回合有效的规则。
 */
internal data class TurnContext(
	val state: BattleState,
	val plannedSkillActions: List<ActionPlan> = emptyList(),
	val resolvedSkillActorIds: Set<String> = emptySet(),
	val protectedActorIds: Set<String> = emptySet(),
	val multiTargetProtectedSideIds: Set<String> = emptySet(),
	val priorityProtectedSideIds: Set<String> = emptySet(),
	val successfulProtectionActorIds: Set<String> = emptySet(),
) {
	/**
	 * 标记某个技能行动者已经完成本回合技能阶段。
	 *
	 * 突袭、快手还击等技能需要知道目标是否“仍在准备行动”；这个集合只服务同一回合内的后续技能，不写入
	 * [BattleState]，避免 replay 快照出现本可由行动序列稳定推导的临时事实。
	 */
	fun markSkillActionResolved(actorId: String): TurnContext =
		copy(resolvedSkillActorIds = resolvedSkillActorIds + actorId)

	/**
	 * 查找目标本回合尚未处理的技能计划。
	 *
	 * 返回 null 表示目标没有技能行动、已经执行过、已经倒下/离场，或本回合选择了替换。调用方据此让依赖目标意图
	 * 的技能失败，而不是把缺失计划误当作普通伤害目标继续结算。
	 */
	fun pendingSkillAction(actorId: String): ActionPlan? =
		plannedSkillActions.firstOrNull { plan ->
			plan.action.actorId == actorId &&
				plan.action.actorId !in resolvedSkillActorIds &&
				state.isActive(plan.action.actorId) &&
				state.participant(plan.action.actorId)?.canBattle() == true
		}

	/**
	 * 判断当前行动之后是否仍存在其它可执行技能计划。
	 *
	 * 广域防守/快速防守只有在队列中还有后续行动时才会建立一侧临时防护。本函数复用排序阶段冻结的计划和已执行集合，
	 * 不根据速度或事件流重新推导“还有谁会行动”，避免同速随机、锁招续行动和蓄力释放在不同入口下出现两套口径。
	 */
	fun hasPendingSkillActionAfter(actorId: String): Boolean =
		plannedSkillActions.any { plan ->
			plan.action.actorId != actorId &&
				plan.action.actorId !in resolvedSkillActorIds &&
				state.isActive(plan.action.actorId) &&
				state.participant(plan.action.actorId)?.canBattle() == true
		}
}
