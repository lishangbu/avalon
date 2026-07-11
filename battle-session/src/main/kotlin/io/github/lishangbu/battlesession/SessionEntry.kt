package io.github.lishangbu.battlesession

import io.github.lishangbu.battleengine.model.BattleInitialState

internal class SessionEntry(
	var snapshot: BattleSessionSnapshot,
	val initialState: BattleInitialState,
) {
	val lock = Any()
	val initialEvents = snapshot.state.events
	val commandCache = mutableMapOf<String, CachedCommand>()
}

internal sealed interface CachedCommand

internal data class CachedTurnCommand(
	val command: TurnCommand,
	val turnRecord: TurnRecord,
) : CachedCommand

internal data class CachedTerminationCommand(
	val command: TerminationCommand,
	val result: TerminationResult,
) : CachedCommand
