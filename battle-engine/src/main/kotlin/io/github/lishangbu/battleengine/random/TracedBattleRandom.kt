package io.github.lishangbu.battleengine.random

import io.github.lishangbu.battleengine.model.BattleRandomTraceEntry

/**
 * 由 replay trace 驱动的严格随机源。
 *
 * 与只按数字脚本回放的随机源不同，该实现会同时校验 `bound` 和 `reason`。如果规则实现改变后多消费、
 * 少消费，或同一位置改成了不同随机分支，回放会在第一处偏离位置失败，而不是继续用后续数字凑出一个
 * 看似合理的结果。
 *
 * 该类主要服务于 `BattleReplayRecorder.replay` 和 CI 对照测试。普通单元测试若只关心随机值顺序，仍然
 * 可以继续使用 `ScriptedBattleRandom` 保持简单。
 */
class TracedBattleRandom(
	private val trace: List<BattleRandomTraceEntry>,
) : BattleRandom {
	private var cursor: Int = 0

	override fun nextInt(bound: Int, reason: String): Int {
		check(cursor < trace.size) { "replay random trace exhausted before consuming $reason" }
		val entry = trace[cursor]
		check(entry.bound == bound) {
			"replay random bound mismatch at sequence ${entry.sequence}: expected ${entry.bound}, actual $bound"
		}
		check(entry.reason == reason) {
			"replay random reason mismatch at sequence ${entry.sequence}: expected ${entry.reason}, actual $reason"
		}
		cursor += 1
		return entry.value
	}

	/**
	 * 判断本回合 trace 是否已经被完整消费。
	 */
	fun isFullyConsumed(): Boolean = cursor == trace.size
}
