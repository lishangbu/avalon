package io.github.lishangbu.battlesession

import io.github.lishangbu.battleengine.BattleEngine
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

interface BattleSessionEngine {
	fun start(initialState: BattleInitialState): BattleState

	fun resolveTurn(
		state: BattleState,
		actions: List<BattleAction>,
		random: BattleRandom,
	): BattleState
}

internal class DefaultBattleSessionEngine(
	private val delegate: BattleEngine = BattleEngine(),
) : BattleSessionEngine {
	override fun start(initialState: BattleInitialState): BattleState = delegate.start(initialState)

	override fun resolveTurn(
		state: BattleState,
		actions: List<BattleAction>,
		random: BattleRandom,
	): BattleState = delegate.resolveTurn(state, actions, random)
}
