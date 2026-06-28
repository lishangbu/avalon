package io.github.lishangbu.battleengine.model

/**
 * 技能命中后尝试附加临时状态的规则片段。
 *
 * 该对象只表达技能效果资料，不直接修改成员状态。畏缩的持续期固定到本回合结束；
 * 混乱的持续内部计数由引擎在效果成功时消费随机数生成，避免把随机逻辑藏在资料层。
 */
data class BattleVolatileStatusApplication(
	val status: BattleVolatileStatus,
	val target: BattleEffectTarget,
	val chancePercent: Int,
) {
	init {
		require(chancePercent in 0..100) { "chancePercent must be between 0 and 100" }
	}
}
