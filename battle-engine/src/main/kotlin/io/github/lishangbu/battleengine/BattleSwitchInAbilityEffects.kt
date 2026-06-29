package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleState

/**
 * 负责成员进入场地时立即触发的结构化特性效果。
 *
 * 出场特性的触发入口有两个：战斗开始时的初始上场，以及替换/强制替换后的新成员上场。两者共享同一套效果分派：
 * 出场能力阶级变化由本类写入，出场天气和场地则委托 [BattleEnvironmentEffects]，保证技能与特性建立环境时使用
 * 相同事件和持续回合延长规则。
 */
internal class BattleSwitchInAbilityEffects(
	private val actionOrdering: BattleActionOrdering,
	private val environmentEffects: BattleEnvironmentEffects,
) {
	/**
	 * 结算战斗开始时所有当前上场成员的出场特性。
	 *
	 * 初始上场不是一次替换行动，但现代规则中“出场时触发”的特性同样会在战斗开始阶段生效。当前按有效速度排序
	 * 触发，戏法空间存在时复用引擎已有的速度比较器反转速度顺序；同速成员保持初始侧和席位顺序。
	 */
	fun applyInitial(state: BattleState): BattleState =
		initialActorIds(state).fold(state) { current, actorId -> apply(current, actorId) }

	/**
	 * 结算单个成员成功进入场地后的出场特性。
	 *
	 * 成员必须当前仍在场且可战斗；如果刚换入后已经被入场陷阱击倒，则不会触发出场特性。当前支持对手当前上场
	 * 成员的能力阶级变化、全场天气覆盖和全场场地覆盖。
	 */
	fun apply(state: BattleState, actorId: String): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!state.isActive(actor.actorId) || !actor.canBattle()) {
			return state
		}
		return actor.abilityEffects
			.fold(state) { current, effect -> applyEffect(current, actor.actorId, effect) }
	}

	/**
	 * 计算战斗开始阶段出场特性的稳定触发顺序。
	 *
	 * 输出只保留成员 ID，避免后续某个成员的出场特性改变环境后导致已排序队列重新计算。有效速度在初始状态上
	 * 一次性计算，包含麻痹、天气速度特性、道具速度倍率和一侧速度修正。
	 */
	private fun initialActorIds(state: BattleState): List<String> =
		state.sides
			.flatMap { side -> side.activeActorIds.mapNotNull { actorId -> state.participant(actorId) } }
			.groupBy { participant -> actionOrdering.effectiveSpeed(state, participant) }
			.toSortedMap(actionOrdering.speedComparator(state))
			.values
			.flatMap { sameSpeedParticipants -> sameSpeedParticipants.map { it.actorId } }

	/**
	 * 将单个结构化特性效果分派到出场阶段实现。
	 *
	 * 只有明确属于 SWITCH_IN 生命周期的效果会改变状态；其它效果返回原状态，避免每新增一种非出场特性都要维护
	 * 一条无意义分支。
	 */
	private fun applyEffect(
		state: BattleState,
		actorId: String,
		effect: BattleAbilityEffect,
	): BattleState =
		when (effect) {
			is BattleAbilityEffect.SwitchInStatStageChange -> applyStatStageChange(state, actorId, effect)
			is BattleAbilityEffect.SwitchInTerrainChange -> environmentEffects.applySwitchInTerrainChange(state, actorId, effect)
			is BattleAbilityEffect.SwitchInWeatherChange -> environmentEffects.applySwitchInWeatherChange(state, actorId, effect)
			else -> state
		}

	/**
	 * 执行出场特性的能力阶级变化。
	 *
	 * 目标集合为触发者对侧当前上场且仍可战斗的成员。每个目标独立夹取 -6..6 的现代能力阶级边界；如果某个目标
	 * 已经达到边界，本次不会写入状态，也不会产生事件。该函数不消费随机数。
	 */
	private fun applyStatStageChange(
		state: BattleState,
		actorId: String,
		effect: BattleAbilityEffect.SwitchInStatStageChange,
	): BattleState {
		val actorSide = state.sideOf(actorId) ?: return state
		val targetActorIds = state.sides
			.filter { it.sideId != actorSide.sideId }
			.flatMap { it.activeParticipants() }
			.filter { it.canBattle() }
			.map { it.actorId }
		return targetActorIds.fold(state) { current, targetActorId ->
			val target = current.participant(targetActorId) ?: return@fold current
			val beforeStage = target.statStage(effect.stat)
			val updated = target.changeStatStage(effect.stat, effect.stageDelta)
			val afterStage = updated.statStage(effect.stat)
			if (beforeStage == afterStage) {
				current
			} else {
				current
					.replaceParticipant(updated)
					.appendEvent(
						BattleEvent.StatStageChanged(
							turnNumber = current.turnNumber,
							actorId = actorId,
							targetActorId = target.actorId,
							stat = effect.stat,
							delta = afterStage - beforeStage,
							currentStage = afterStage,
						),
					)
			}
		}
	}
}
