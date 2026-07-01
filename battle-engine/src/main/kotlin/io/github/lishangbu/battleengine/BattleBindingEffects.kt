package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleVolatileStatus

/**
 * 束缚类临时状态的持续伤害、换人限制和来源离场清理。
 *
 * 束缚和普通“回合末递减状态”不同：它有来源成员、会在回合末造成间接伤害、会阻止目标主动替换，并且会在来源
 * 成员离场时提前解除。因此它不能只放在回合末临时状态倒计时推进器里；主动换人、强制换人和回合末扣血都需要
 * 读取同一套“来源是否仍有效”和“如何清除束缚”的规则。
 *
 * 本类不负责把混乱/束缚写入目标，那仍由 [BattleVolatileStatusEffects] 在技能附加状态阶段处理；这里只维护
 * 已经存在的束缚状态在后续阶段如何生效和结束。
 */
internal class BattleBindingEffects(
	private val damageResultEffects: BattleEndTurnDamageResultEffects,
) {
	/**
	 * 判断成员当前束缚来源是否仍在场并可战斗。
	 *
	 * 该判断同时服务主动替换限制和回合末束缚伤害。保持单一实现可以避免“换人认为束缚仍有效，但回合末认为
	 * 已经失效”这类状态矛盾。函数只读取快照，不追加事件。
	 */
	fun isBindingSourceActive(state: BattleState, participant: BattleParticipant): Boolean {
		val sourceActorId = participant.boundByActorId ?: return false
		val source = state.participant(sourceActorId) ?: return false
		return source.canBattle() && state.isActive(sourceActorId)
	}

	/**
	 * 清除指定来源成员维持的所有束缚。
	 *
	 * 当束缚来源主动换下或被强制换下时，来源不再“在场维持”束缚，所有由它施加的束缚都应立刻结束。这里遍历
	 * 完整队伍而不是只看当前上场成员，是为了让双打、强制替换或未来多目标束缚场景中，离场来源维持的多个目标
	 * 都能一致解除。每个被解除的目标都会追加 [BattleEvent.VolatileStatusCleared]。
	 */
	fun clearBindingsFromSource(state: BattleState, sourceActorId: String): BattleState =
		state.sides
			.flatMap { it.participants }
			.filter { it.boundByActorId == sourceActorId && it.bindingTurnsRemaining > 0 }
			.fold(state) { current, participant -> clearBindingState(current, participant) }

	/**
	 * 处理束缚类临时状态的回合末伤害和持续时间。
	 *
	 * 来源失效时只解除束缚，不造成伤害。来源仍有效时，目标受到最大 HP 1/8 的间接伤害；造成伤害后递减持续回合，
	 * 如果本次是最后一回合，就在扣血事件之后、低体力回复道具之前追加束缚解除事件。
	 */
	fun applyEndTurnDamage(state: BattleState): BattleState =
		state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				if (latest.bindingTurnsRemaining <= 0) {
					return@fold current
				}
				if (!isBindingSourceActive(current, latest)) {
					return@fold clearBindingState(current, latest)
				}
				if (!latest.canBattle() || latest.hasIndirectDamageImmunity()) {
					return@fold current
				}
				val sourceActorId = requireNotNull(latest.boundByActorId) { "binding source must be present" }
				val turnsRemainingBefore = latest.bindingTurnsRemaining
				val damage = (latest.maxHp / BINDING_DAMAGE_DENOMINATOR).coerceAtLeast(1)
				val damaged = latest.receiveDamage(damage).decrementBindingEndTurn()
				damageResultEffects.apply(
					state = current,
					damaged = damaged,
					event = BattleEvent.BindingDamageApplied(
						turnNumber = current.turnNumber,
						actorId = latest.actorId,
						sourceActorId = sourceActorId,
						amount = damage,
						turnsRemainingBefore = turnsRemainingBefore,
					),
					afterEvent = { afterDamage ->
						if (turnsRemainingBefore == 1) {
							afterDamage.appendEvent(
								BattleEvent.VolatileStatusCleared(
									turnNumber = current.turnNumber,
									actorId = latest.actorId,
									status = BattleVolatileStatus.BINDING,
								),
							)
						} else {
							afterDamage
						}
					},
				)
			}

	/**
	 * 清除单个成员的束缚并追加解除事件。
	 *
	 * 调用方已经确认目标确实存在束缚；本函数只做不可变状态替换和事件追加。它不判断来源成员、不处理伤害，也不
	 * 消费随机数，因此可以同时供“来源离场清理”和“回合末自然结束”两个场景复用。
	 */
	private fun clearBindingState(state: BattleState, participant: BattleParticipant): BattleState =
		state
			.replaceParticipant(participant.clearBinding())
			.appendEvent(
				BattleEvent.VolatileStatusCleared(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					status = BattleVolatileStatus.BINDING,
				),
			)

	private companion object {
		/**
		 * 束缚类持续伤害使用最大 HP 的 1/8。
		 */
		private const val BINDING_DAMAGE_DENOMINATOR = 8
	}
}
