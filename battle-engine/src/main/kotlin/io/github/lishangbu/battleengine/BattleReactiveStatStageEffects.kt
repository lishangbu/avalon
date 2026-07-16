package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleState

/** 结算由已经发生的对手能力阶级提升触发的复制特性。 */
internal class BattleReactiveStatStageEffects {
	fun copyOpponentIncreases(state: BattleState, eventStartIndex: Int): BattleState {
		val increases = state.events.drop(eventStartIndex)
			.filterIsInstance<BattleEvent.StatStageChanged>()
			.filter { it.delta > 0 }
		return increases.fold(state) { current, increase ->
			val sourceSideId = current.sideOf(increase.targetActorId)?.sideId ?: return@fold current
			val holders = current.sides
				.filterNot { it.sideId == sourceSideId }
				.flatMap { it.activeParticipants() }
				.filter { holder ->
					holder.canBattle() && holder.abilityEffects.any {
						it is BattleAbilityEffect.OpponentStatStageIncreaseCopy
					}
				}
			holders.fold(current) { holderState, holderSnapshot ->
				val holder = holderState.participant(holderSnapshot.actorId) ?: return@fold holderState
				val before = holder.statStage(increase.stat)
				val updated = holder.changeStatStage(increase.stat, increase.delta)
				val after = updated.statStage(increase.stat)
				if (before == after) {
					holderState
				} else {
					holderState.replaceParticipant(updated).appendEvent(
						BattleEvent.StatStageChanged(
							turnNumber = holderState.turnNumber,
							actorId = holder.actorId,
							targetActorId = holder.actorId,
							stat = increase.stat,
							delta = after - before,
							currentStage = after,
						),
					)
				}
			}
		}
	}
}
