package io.github.lishangbu.battleengine.model

/**
 * 技能命中后尝试附加主要异常状态的规则片段。
 *
 * 该对象来自规则快照或测试用例，不直接来自数据库实体。`chancePercent` 为 100 时不消费随机数；
 * 小于 100 时通过战斗随机源消费一次 1..100 掷点。本对象只描述“想附加什么状态、作用到谁、概率是多少”，
 * 目标是否已有主要异常状态、属性/场地/特性/道具免疫以及状态写入后的道具解除，都由主要异常状态处理器统一判定。
 */
data class BattleStatusApplication(
	val status: BattleMajorStatus,
	val target: BattleEffectTarget,
	val chancePercent: Int,
) {
	init {
		require(chancePercent in 0..100) { "chancePercent must be between 0 and 100" }
	}
}
