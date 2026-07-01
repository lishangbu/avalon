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
		val afterSwitches = switchResolution.resolve(started, actions.filterIsInstance<BattleAction.SwitchParticipant>(), random)
		if (afterSwitches.result != null) {
			return afterSwitches
		}
		val skillActions = actionPlanner.skillActionsForTurn(afterSwitches, actions.filterIsInstance<BattleAction.UseSkill>())
		val orderedActions = actionPlanner.orderSkillActions(afterSwitches, skillActions, random)
		val resolvedContext = orderedActions.fold(TurnContext(afterSwitches)) { current, plan ->
			if (current.state.result != null) current else skillUseResolution.resolve(current, plan, random)
		}
		return finishTurnAfterActions(resolvedContext, nextTurnNumber)
	}

	/**
	 * 结算所有行动之后的回合末流水线。
	 *
	 * 技能阶段结束时 [TurnContext] 还持有本回合临时保护集合；这个集合不能写入 [BattleState]，因为保护只对当前
	 * 回合剩余技能有效。回合末第一步用它重置连续保护链，然后再进入真正的回合末效果。每个阶段前都检查
	 * `result`，原因是持续伤害、天气伤害或回合上限都可能直接结束战斗；战斗一旦结束，后续阶段不能继续追加事件。
	 */
	private fun finishTurnAfterActions(context: TurnContext, nextTurnNumber: Int): BattleState {
		val resolved = endTurnVolatileStatuses.resetProtectionChains(
			state = context.state,
			successfulProtectionActorIds = context.successfulProtectionActorIds,
		)
		val afterEndTurnEffects = resolved.result?.let { resolved } ?: endTurnEffects.apply(resolved)
		val afterEndTurnVolatileStatuses = afterEndTurnEffects.result?.let { afterEndTurnEffects }
			?: endTurnVolatileStatuses.clearEndTurnOnlyStatuses(afterEndTurnEffects)
		val afterEndTurnVolatileStatusDurations = afterEndTurnVolatileStatuses.result?.let { afterEndTurnVolatileStatuses }
			?: endTurnVolatileStatuses.advanceEndTurnDurations(afterEndTurnVolatileStatuses)
		val afterEnvironmentDurations = afterEndTurnVolatileStatusDurations.result?.let { afterEndTurnVolatileStatusDurations }
			?: environmentEffects.advanceDurations(afterEndTurnVolatileStatusDurations)
		val afterSideConditionDurations = afterEnvironmentDurations.result?.let { afterEnvironmentDurations }
			?: afterEnvironmentDurations.advanceSideConditionDurations()
		val afterTurnLimit = afterSideConditionDurations.result?.let { afterSideConditionDurations }
			?: endTurnEffects.applyTurnLimit(afterSideConditionDurations)
		return afterTurnLimit.result?.let { afterTurnLimit }
			?: afterTurnLimit.appendEvent(BattleEvent.TurnEnded(nextTurnNumber))
	}
}
