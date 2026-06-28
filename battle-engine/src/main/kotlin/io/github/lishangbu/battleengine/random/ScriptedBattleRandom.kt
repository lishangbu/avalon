package io.github.lishangbu.battleengine.random

/**
 * 由测试脚本驱动的确定性随机源。
 *
 * `values` 中的每个数字按顺序被消费，且必须落在当前请求的 `[0, bound)` 范围内。
 * 如果脚本耗尽或数值越界，随机源会立即失败，帮助 fixture 暴露“实现多消费或少消费了随机数”的问题。
 */
class ScriptedBattleRandom(
	private val values: List<Int>,
) : BattleRandom {
	private var cursor: Int = 0
	private val consumedReasons = mutableListOf<String>()

	override fun nextInt(bound: Int, reason: String): Int {
		require(bound > 0) { "bound must be positive" }
		check(cursor < values.size) { "scripted random exhausted before consuming $reason" }
		val value = values[cursor]
		check(value in 0 until bound) { "scripted random value $value is outside [0, $bound) for $reason" }
		cursor += 1
		consumedReasons += reason
		return value
	}

	/**
	 * 返回已经消费的随机原因，用于对照测试断言消费顺序。
	 */
	fun consumedReasons(): List<String> = consumedReasons.toList()

	/**
	 * 判断脚本值是否已经全部消费。
	 */
	fun isFullyConsumed(): Boolean = cursor == values.size
}
