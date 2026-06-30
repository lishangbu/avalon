package io.github.lishangbu.battleengine.model

/**
 * 技能命中后尝试附加主要异常状态的规则片段。
 *
 * 该对象来自规则快照或测试用例，不直接来自数据库实体。`chancePercent` 为 100 时不消费随机数；
 * 小于 100 时通过战斗随机源消费一次 1..100 掷点。第一批只处理“目标没有主要异常状态时附加成功”的简单规则。
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
