package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleState

/** 返回成员本次上场后已经经历的回合数，并把当前回合计为第一回合。 */
internal fun BattleState.activeTurnsIncludingCurrent(participant: BattleParticipant): Int {
	val switchInTurn = events.filterIsInstance<BattleEvent.ParticipantSwitched>()
		.lastOrNull { it.nextActorId == participant.actorId }
		?.turnNumber
	return if (switchInTurn == null) {
		turnNumber.coerceAtLeast(1)
	} else {
		(turnNumber - switchInTurn + 1).coerceAtLeast(1)
	}
}
