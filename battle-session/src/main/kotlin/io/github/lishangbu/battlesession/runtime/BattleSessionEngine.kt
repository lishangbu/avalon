package io.github.lishangbu.battlesession.runtime

import io.github.lishangbu.battleengine.BattleEngine
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 隔离 Session Runtime 与战斗引擎实现，供确定性测试替换回合结算行为。
 */
internal interface BattleSessionEngine {
	fun start(initialState: BattleInitialState): BattleState

	fun resolveTurn(
		state: BattleState,
		actions: List<BattleAction>,
		random: BattleRandom,
	): BattleState

	/** 使用正式 [BattleEngine] 执行会话启动与回合结算。 */
	class Default(
		private val delegate: BattleEngine = BattleEngine(),
	) : BattleSessionEngine {
		override fun start(initialState: BattleInitialState): BattleState = delegate.start(initialState)

		override fun resolveTurn(
			state: BattleState,
			actions: List<BattleAction>,
			random: BattleRandom,
		): BattleState = delegate.resolveTurn(state, actions, random)
	}
}
