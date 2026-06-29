package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 单个回合内技能行动的收集与排序。
 *
 * 该组件位于回合生命周期的“行动准备”阶段：它把玩家提交的技能行动、锁招续行动和蓄力释放行动合并成统一队列，
 * 然后根据有效优先度、有效速度和同速随机键得到执行顺序。它不执行技能、不消耗 PP、不改写状态、不追加事件；
 * 输出的 [ActionPlan] 只是 [BattleEngine] 后续技能结算阶段的输入。
 *
 * 排序不变量：先比较有效优先度，再比较当前环境下的有效速度；同一优先度和速度内消费确定性随机数打破平手。
 * 锁招和蓄力行动会附带 [SkillActionSource]，让执行阶段决定是否消耗 PP、是否解除蓄力状态，以及被行动前状态阻止
 * 时需要如何收尾。
 */
internal class BattleTurnActionPlanner(
	private val actionOrdering: BattleActionOrdering,
) {
	/**
	 * 根据当前状态补齐本回合应参与技能阶段的行动。
	 *
	 * 锁招续行动和蓄力释放行动来自当前上场成员的运行态，并会覆盖同一成员提交的普通技能选择。这里只生成候选
	 * 行动，不判断行动是否会被睡眠、畏缩、挑衅等规则阻止。
	 */
	internal fun skillActionsForTurn(
		state: BattleState,
		submittedActions: List<BattleAction.UseSkill>,
	): List<SkillActionInput> {
		val lockedActions = state.sides
			.flatMap { it.activeParticipants() }
			.filter { it.canBattle() && it.lockedMoveTurnsRemaining > 0 }
			.mapNotNull { actor ->
				val skillId = actor.lockedMoveSkillId ?: return@mapNotNull null
				val targetActorId = actor.lockedMoveTargetActorId ?: return@mapNotNull null
				SkillActionInput(
					action = BattleAction.UseSkill(actor.actorId, skillId = skillId, targetActorId = targetActorId),
					source = SkillActionSource.LOCKED_CONTINUATION,
				)
			}
		val chargingActions = state.sides
			.flatMap { it.activeParticipants() }
			.filter { it.canBattle() && it.chargingTurnsRemaining > 0 }
			.mapNotNull { actor ->
				val skillId = actor.chargingSkillId ?: return@mapNotNull null
				val targetActorId = actor.chargingTargetActorId ?: return@mapNotNull null
				SkillActionInput(
					action = BattleAction.UseSkill(actor.actorId, skillId = skillId, targetActorId = targetActorId),
					source = SkillActionSource.CHARGED_RELEASE,
				)
			}
		val forcedActorIds = (lockedActions + chargingActions).map { it.action.actorId }.toSet()
		return submittedActions
			.filterNot { it.actorId in forcedActorIds }
			.map { SkillActionInput(it, source = SkillActionSource.SUBMITTED) } + lockedActions + chargingActions
	}

	/**
	 * 将技能候选行动转换为可执行计划并排序。
	 *
	 * 该阶段会校验行动者和技能槽存在，并提前阻止被讲究类道具锁定到其它技能的提交行动。有效优先度上下文由
	 * [BattleActionOrdering] 计算，后续精神场地、先制阻挡和恶属性免疫读取同一份上下文。
	 */
	internal fun orderSkillActions(
		state: BattleState,
		actions: List<SkillActionInput>,
		random: BattleRandom,
	): List<ActionPlan> {
		val plans = actions.map { input ->
			val action = input.action
			val actor = requireNotNull(state.participant(action.actorId)) { "actor not found: ${action.actorId}" }
			val skill = requireNotNull(actor.skillSlot(action.skillId)) { "skill not found: ${action.skillId}" }
			if (input.source == SkillActionSource.SUBMITTED) {
				require(!actor.choiceLockedToAnotherSkill(action.skillId)) {
					"choice locked to skill: ${actor.choiceLockedSkillId}"
				}
			}
			ActionPlan(
				action = action,
				actor = actor,
				skill = skill,
				source = input.source,
				priorityContext = actionOrdering.skillPriorityContext(actor, skill),
			)
		}
		val speedComparator = actionOrdering.speedComparator(state)
		val orderComparator = compareByDescending<Pair<Int, Int>> { it.first }
			.thenComparator { left, right -> speedComparator.compare(left.second, right.second) }
		return plans
			.groupBy { it.priorityContext.effectivePriority to actionOrdering.effectiveSpeed(state, it.actor) }
			.toSortedMap(orderComparator)
			.values
			.flatMap { sameOrderPlans ->
				if (sameOrderPlans.size == 1) {
					sameOrderPlans
				} else {
					sameOrderPlans.sortedByRandomTieBreak(random) { "speed tie for ${it.actor.actorId}" }
				}
			}
	}
}

internal data class SkillActionInput(
	val action: BattleAction.UseSkill,
	val source: SkillActionSource,
)

internal data class ActionPlan(
	val action: BattleAction.UseSkill,
	val actor: BattleParticipant,
	val skill: BattleSkillSlot,
	val source: SkillActionSource,
	val priorityContext: SkillPriorityContext,
)

internal enum class SkillActionSource {
	SUBMITTED,
	LOCKED_CONTINUATION,
	CHARGED_RELEASE,
}
