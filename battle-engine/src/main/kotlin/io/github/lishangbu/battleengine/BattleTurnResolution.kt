package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 完整单回合生命周期编排器。
 *
 * [BattleEngine] 负责创建一场战斗和装配规则 resolver，本类负责“战斗已经开始后，一个回合如何推进”。这个拆分
 * 的目标不是把战斗引擎改成事件总线，也不是让回合阶段变成可插拔插件；现代规则最敏感的部分仍然是固定顺序，
 * 因此这里继续用直接函数调用表达阶段边界：
 * 1. 递增回合数并追加 [BattleEvent.TurnStarted]。
 * 2. 先结算主动替换，替换可能触发入场特性、入场陷阱和胜负检查。
 * 3. 从提交行动、锁招续行动和蓄力释放行动中收集技能阶段候选。
 * 4. 按有效优先度、速度和同速随机键排序。
 * 5. 逐个执行技能行动；某个行动导致战斗结束时，后续行动不会继续执行。
 * 6. 进入回合末流水线，按异常/天气/场地/道具/持续时间/回合上限顺序收口。
 *
 * 这个类只编排阶段，不直接实现命中、伤害、状态、特性、道具或场地细节。那些规则仍由更小的 resolver 负责。
 * 这样做能把 [BattleEngine] 从“知道所有阶段细节的巨型对象”降到“装配依赖并暴露公共 API 的入口”，同时保留
 * 测试中已经验证过的阶段顺序。
 */
internal class BattleTurnResolution(
	private val switchResolution: BattleSwitchResolution,
	private val actionPlanner: BattleTurnActionPlanner,
	private val skillUseResolution: BattleSkillUseResolution,
	private val reactiveStatStageEffects: BattleReactiveStatStageEffects,
	private val endTurnVolatileStatuses: BattleEndTurnVolatileStatuses,
	private val endTurnEffects: BattleEndTurnEffects,
	private val environmentEffects: BattleEnvironmentEffects,
) {
	/**
	 * 结算一个完整回合。
	 *
	 * 输入校验仍放在回合入口，而不是散落到替换或技能 resolver 中：调用者不能给已经结束的战斗继续提交行动，
	 * 同一个成员也不能在同一回合提交两份显式行动。锁招和蓄力释放不是“提交行动”，它们由 [BattleTurnActionPlanner]
	 * 从成员运行态派生，因此不会触发这里的一人一行动校验。
	 */
	fun resolve(state: BattleState, actions: List<BattleAction>, random: BattleRandom): BattleState {
		require(state.result == null) { "battle already ended" }
		require(actions.map { it.actorId }.toSet().size == actions.size) {
			"each actor can submit at most one action per turn"
		}
		val nextTurnNumber = state.turnNumber + 1
		val started = state
			.copy(turnNumber = nextTurnNumber)
			.appendEvent(BattleEvent.TurnStarted(nextTurnNumber))
		val afterSwitches = reactiveStatStageEffects.copyOpponentIncreases(
			switchResolution.resolve(started, actions.filterIsInstance<BattleAction.SwitchParticipant>(), random),
			started.events.size,
		)
		if (afterSwitches.result != null) {
			return afterSwitches
		}
		val skillActions = actionPlanner.skillActionsForTurn(afterSwitches, actions.filterIsInstance<BattleAction.UseSkill>())
		val orderedActions = actionPlanner.orderSkillActions(afterSwitches, skillActions, random)
		val afterOrderItems = orderedActions.filter(ActionPlan::consumesOrderItem).fold(afterSwitches) { current, plan ->
			val actor = current.participant(plan.action.actorId) ?: return@fold current
			current.replaceParticipant(actor.consumeHeldItem())
		}
		val resolvedContext = orderedActions.fold(TurnContext(afterOrderItems, plannedSkillActions = orderedActions)) { current, plan ->
			if (current.state.result != null) {
				current
			} else {
				val prepared = current.copy(state = current.state.applyTerastallization(plan.action))
				val resolved = skillUseResolution.resolve(prepared, plan, random)
				val afterReactiveEffects = resolved.copy(
					state = reactiveStatStageEffects.copyOpponentIncreases(
						resolved.state,
						prepared.state.events.size,
					),
				)
				val usedSkill = afterReactiveEffects.state.events.drop(prepared.state.events.size)
					.filterIsInstance<BattleEvent.SkillUsed>()
					.any { it.actorId == plan.action.actorId }
				val afterAccuracyBoost = if (usedSkill) {
					val actor = afterReactiveEffects.state.participant(plan.action.actorId)
					if (actor != null && actor.nextSkillAccuracyMultiplier != 1.0) {
						afterReactiveEffects.copy(
							state = afterReactiveEffects.state.replaceParticipant(actor.copy(nextSkillAccuracyMultiplier = 1.0)),
						)
					} else {
						afterReactiveEffects
					}
				} else {
					afterReactiveEffects
				}
				afterAccuracyBoost.markSkillActionResolved(plan.action.actorId)
			}
		}
		return finishTurnAfterActions(resolvedContext, nextTurnNumber, random)
	}

	/**
	 * 结算所有行动之后的回合末流水线。
	 *
	 * 技能阶段结束时 [TurnContext] 还持有本回合临时保护集合；这个集合不能写入 [BattleState]，因为保护只对当前
	 * 回合剩余技能有效。回合末第一步用它重置连续保护链，然后再进入真正的回合末效果。每个阶段前都检查
	 * `result`，原因是持续伤害、天气伤害或回合上限都可能直接结束战斗；战斗一旦结束，后续阶段不能继续追加事件。
	 */
	private fun finishTurnAfterActions(
		context: TurnContext,
		nextTurnNumber: Int,
		random: BattleRandom,
	): BattleState {
		val resolved = endTurnVolatileStatuses.resetProtectionChains(
			state = context.state,
			successfulProtectionActorIds = context.successfulProtectionActorIds,
		)
		val afterEndTurnEffects = resolved.thenIfBattleContinues { endTurnEffects.apply(it, random) }
		val afterEndTurnVolatileStatuses = afterEndTurnEffects.thenIfBattleContinues(
			endTurnVolatileStatuses::clearEndTurnOnlyStatuses,
		)
		val afterEndTurnVolatileStatusDurations = afterEndTurnVolatileStatuses.thenIfBattleContinues(
			endTurnVolatileStatuses::advanceEndTurnDurations,
		)
		val afterEnvironmentDurations = afterEndTurnVolatileStatusDurations.thenIfBattleContinues(
			environmentEffects::advanceDurations,
		)
		val afterSideConditionDurations = afterEnvironmentDurations.thenIfBattleContinues(
			BattleState::advanceSideConditionDurations,
		)
		val afterTurnLimit = afterSideConditionDurations.thenIfBattleContinues(endTurnEffects::applyTurnLimit)
		return afterTurnLimit.thenIfBattleContinues { it.appendEvent(BattleEvent.TurnEnded(nextTurnNumber)) }
	}

	/**
	 * 回合末流水线的短路 helper。
	 *
	 * 回合末每个阶段都可能产生胜负结果，例如异常伤害、天气伤害或回合上限。结果一旦出现，后续阶段不能继续追加
	 * 事件，否则 replay 会看到“战斗已经结束之后又清状态/推进天气”的伪事实。这个 helper 只表达同一个短路规则，
	 * 不改变阶段顺序，也不把阶段注册成可变列表；调用点仍然显式列出每一步现代规则顺序。
	 */
	private fun BattleState.thenIfBattleContinues(next: (BattleState) -> BattleState): BattleState =
		if (result == null) next(this) else this
}
