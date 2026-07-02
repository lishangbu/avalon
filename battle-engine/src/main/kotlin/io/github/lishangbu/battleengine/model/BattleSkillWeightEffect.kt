package io.github.lishangbu.battleengine.model

/**
 * 技能成功后对成员当前体重产生的临时修正。
 *
 * 该模型只描述“本场在场期间用于体重相关技能的有效体重如何变化”，不修改基础资料体重。现代规则里这类效果会被
 * 低踢、打草结、重磅冲撞和高温重压读取；成员离场时会清除该临时修正，因此不要把它持久化回资料库或初始队伍。
 *
 * [reduction] 和 [minimumWeight] 使用 [BattleParticipant.weight] 相同的整数刻度；本项目基础体重资料使用十分之一
 * 千克，因此 100kg 写作 1000，最低 0.1kg 写作 1。[requiredChangedStat] 用于表达“只有指定能力阶级在本次技能中
 * 成功变化后才生效”的公开规则，例如速度已到 +6 时再次使用不会继续减轻体重。
 */
data class BattleSkillWeightEffect(
	val target: BattleEffectTarget,
	val reduction: Int,
	val minimumWeight: Int = 1,
	val requiredChangedStat: BattleStat? = null,
) {
	init {
		require(reduction > 0) { "reduction must be positive" }
		require(minimumWeight > 0) { "minimumWeight must be positive" }
	}
}
