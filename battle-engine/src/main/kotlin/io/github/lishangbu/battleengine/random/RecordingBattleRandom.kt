package io.github.lishangbu.battleengine.random

import io.github.lishangbu.battleengine.model.BattleRandomTraceEntry

/**
 * 记录随机消费过程的装饰器。
 *
 * 生产环境、模拟器或测试仍然可以选择自己的随机源；该类只包在外层，把每次 `nextInt` 的上界、原因和返回值
 * 记录为 [BattleRandomTraceEntry]。它不会改变随机源行为，也不会提前窥探后续值，因此可以安全用于真实
 * 对局运行记录和公开对照测试复盘。
 *
 * 记录粒度是“一个回合一个实例”。调用方应在每次回合结算前创建新的 recorder，这样 trace 的 `sequence`
 * 能从 1 开始，报告和错误定位都会更清晰。若需要跨回合保存完整记录，应把每回合 trace 放入
 * `BattleReplayTurn`，而不是复用同一个 recorder。
 */
class RecordingBattleRandom(
	private val delegate: BattleRandom,
) : BattleRandom {
	private val trace = mutableListOf<BattleRandomTraceEntry>()

	override fun nextInt(bound: Int, reason: String): Int {
		val value = delegate.nextInt(bound, reason)
		trace += BattleRandomTraceEntry(
			sequence = trace.size + 1,
			bound = bound,
			reason = reason,
			value = value,
		)
		return value
	}

	/**
	 * 返回当前已经记录的随机消费 trace。
	 *
	 * 返回值是不可变副本，调用方可以安全地把它放进 replay 对象中；后续随机消费不会修改已经返回的列表。
	 */
	fun trace(): List<BattleRandomTraceEntry> = trace.toList()
}
