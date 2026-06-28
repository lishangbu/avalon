package io.github.lishangbu.battleengine.model

/**
 * 技能命中后造成能力阶级变化的规则片段。
 *
 * `stageDelta` 表示单次效果变化值，并会在成员状态里夹取到 -6..6。第一批只处理使用者和选中目标，
 * 不处理替身、白雾、清除之烟、强行等复杂拦截或跳过附加效果的规则。
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
