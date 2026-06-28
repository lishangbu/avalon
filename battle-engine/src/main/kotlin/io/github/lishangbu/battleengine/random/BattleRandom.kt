package io.github.lishangbu.battleengine.random

/**
 * 战斗随机源。
 *
 * 所有需要随机数的规则都必须通过这个接口消费随机值，并提供 `reason` 说明消费场景。
 * 对照测试可以使用脚本随机源固定每一次消费；线上或模拟运行可以使用其它实现。
 */
interface BattleRandom {
	/**
	 * 生成 `[0, bound)` 范围内的整数。
	 *
	 * @param bound 上界，必须大于 0。
	 * @param reason 随机消费原因，例如命中、伤害随机浮动或同速排序。
	 */
	fun nextInt(bound: Int, reason: String): Int
}
