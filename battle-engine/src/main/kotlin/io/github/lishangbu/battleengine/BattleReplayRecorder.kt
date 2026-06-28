package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleReplay
import io.github.lishangbu.battleengine.model.BattleReplayTurn
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom
import io.github.lishangbu.battleengine.random.RecordingBattleRandom
import io.github.lishangbu.battleengine.random.TracedBattleRandom

/**
 * 战斗 replay 录制与严格回放入口。
 *
 * 该类围绕 [BattleEngine] 做很薄的一层编排：录制时，它先启动战斗，再逐回合调用 `resolveTurn`，并把每个
 * 回合提交的行动、随机消费 trace 和新增事件切成独立片段；回放时，它重新启动同一初始状态，再使用
 * trace 中保存的随机值逐回合结算，并校验事件片段和最终状态完全一致。
 *
 * 这里没有数据库、文件系统或后台任务依赖。上层应用可以把 [BattleReplay] 序列化到测试报告、管理端运行记录
 * 或 CI artifact 中，但序列化格式不属于引擎核心契约。引擎核心只保证 replay 对象中的结构化事实足以完成
 * 一次确定性复盘。
 *
 * 如果未来引入主动使用道具、复杂特殊机制或更多战斗模式，该类不应理解那些规则本身；只要新规则继续通过
 * [BattleAction]、[BattleEvent] 和 [BattleRandom] 暴露输入、事实和随机消费，replay 录制逻辑就可以保持
 * 不变。真正需要改动的通常是具体行动子类型或事件模型。
 */
class BattleReplayRecorder(
	private val engine: BattleEngine = BattleEngine(),
) {
	/**
	 * 录制一场从初始快照开始、按给定行动序列结算的战斗。
	 *
	 * @param initialState 战斗开始前的冻结快照。
	 * @param turns 每个元素表示一回合调用方提交给引擎的行动列表。
	 * @param random 真实随机源或测试脚本随机源；录制器会在每回合外层包一层 trace recorder。
	 * @return 可严格回放的 replay 对象。
	 * @throws IllegalArgumentException 当调用方尝试在已经结束的战斗后继续提交回合时抛出。
	 */
	fun record(
		initialState: BattleInitialState,
		turns: List<List<BattleAction>>,
		random: BattleRandom,
	): BattleReplay {
		var current = engine.start(initialState)
		val initialEvents = current.events
		val replayTurns = mutableListOf<BattleReplayTurn>()

		turns.forEachIndexed { index, actions ->
			require(current.result == null) { "battle already ended before replay turn ${index + 1}" }
			val turnRandom = RecordingBattleRandom(random)
			val beforeEventCount = current.events.size
			val resolved = engine.resolveTurn(current, actions, turnRandom)
			replayTurns += BattleReplayTurn(
				turnNumber = resolved.turnNumber,
				submittedActions = actions,
				randomTrace = turnRandom.trace(),
				events = resolved.events.drop(beforeEventCount),
			)
			current = resolved
		}

		return BattleReplay(
			initialState = initialState,
			initialEvents = initialEvents,
			turns = replayTurns,
			finalState = current,
		)
	}

	/**
	 * 严格回放并校验一份 replay。
	 *
	 * 方法会重新执行 `BattleEngine.start` 和每个回合的 `resolveTurn`。每回合使用 [TracedBattleRandom]，
	 * 因此随机消费的上界、原因和值都必须和录制时一致。结算后还会比较本回合新增事件和最终状态；任何差异
	 * 都说明规则实现、输入快照或 replay 本身已经不一致。
	 *
	 * @return 重新结算得到的最终状态；成功返回即表示它与 replay 中保存的 `finalState` 完全一致。
	 * @throws IllegalStateException 当启动事件、随机 trace、回合事件或最终状态任一处不一致时抛出。
	 */
	fun replay(replay: BattleReplay): BattleState {
		var current = engine.start(replay.initialState)
		check(current.events == replay.initialEvents) { "replay initial events differ from recorded events" }

		replay.turns.forEach { turn ->
			check(current.result == null) { "battle already ended before replay turn ${turn.turnNumber}" }
			val random = TracedBattleRandom(turn.randomTrace)
			val beforeEventCount = current.events.size
			val resolved = engine.resolveTurn(current, turn.submittedActions, random)
			check(random.isFullyConsumed()) {
				"replay random trace for turn ${turn.turnNumber} was not fully consumed"
			}
			val producedEvents: List<BattleEvent> = resolved.events.drop(beforeEventCount)
			check(producedEvents == turn.events) { "replay events differ at turn ${turn.turnNumber}" }
			current = resolved
		}

		check(current == replay.finalState) { "replay final state differs from recorded final state" }
		return current
	}
}
