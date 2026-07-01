package io.github.lishangbu.battleengine.model

/**
 * 技能命中后造成能力阶级变化的规则片段。
 *
 * `stageDelta` 表示单次效果变化值，并会在成员状态里夹取到 -6..6。[target] 使用逐目标结算里的相对落点：
 * `USER` 表示技能使用者，`TARGET` 表示当前正在处理的实际目标。替身、能力变化免疫、清除/复制/交换/取反等
 * 不同语义不会塞进这个简单片段，而是通过专门的状态操作或技能效果对象表达，避免一个数据类承担所有后效规则。
 */
data class BattleStatStageEffect(
	val stat: BattleStat,
	val target: BattleEffectTarget,
	val stageDelta: Int,
	val chancePercent: Int,
) {
	init {
		require(stageDelta in -6..6 && stageDelta != 0) { "stageDelta must be between -6 and 6 and not zero" }
		require(chancePercent in 0..100) { "chancePercent must be between 0 and 100" }
	}
}
