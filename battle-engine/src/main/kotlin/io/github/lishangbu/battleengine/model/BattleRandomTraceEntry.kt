package io.github.lishangbu.battleengine.model

/**
 * 一次战斗随机消费的可复盘记录。
 *
 * 战斗引擎中所有随机分支都会通过 `BattleRandom.nextInt` 消费随机值。仅保存最终随机值不足以支撑稳定
 * replay，因为实现如果多消费、少消费，或把同一个随机值用于不同规则分支，都可能在最终 HP 恰好一致时被
 * 掩盖。因此 trace 同时保存本次消费的顺序号、上界、原因和实际返回值。
 *
 * `sequence` 是单个回合内从 1 开始的顺序号，不跨回合累计。这样前端或 CI 报告可以直接把随机消费挂到
 * 对应回合下展示，也能避免长战斗中全局序号过大而不利于人工比对。
 *
 * @property sequence 单回合内的随机消费顺序，必须从 1 开始连续递增。
 * @property bound 调用 `nextInt` 时传入的上界，表示合法值范围为 `[0, bound)`。
 * @property reason 引擎声明的消费原因，例如命中、击中要害、伤害浮动或同速排序。
 * @property value 随机源实际返回的值，必须落在 `[0, bound)` 范围内。
 */
data class BattleRandomTraceEntry(
	val sequence: Int,
	val bound: Int,
	val reason: String,
	val value: Int,
) {
	init {
		require(sequence > 0) { "sequence must be positive" }
		require(bound > 0) { "bound must be positive" }
		require(reason.isNotBlank()) { "reason must not be blank" }
		require(value in 0 until bound) { "value must be inside [0, bound)" }
	}
}
