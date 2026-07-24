package io.github.lishangbu.battleengine.model

/**
 * 一组可被临时压制、随后原样恢复的被动效果。
 *
 * 状态只保存一份完整效果列表，并用 [suppressed] 表达它当前是否可执行，避免调用方分别搬运“活动列表”和
 * “暂存列表”而制造两份数据同时存在的非法组合。
 */
data class BattlePassiveEffectState<T>(
	val effects: List<T> = emptyList(),
	val suppressed: Boolean = false,
) {
	init {
		require(!suppressed || effects.isNotEmpty()) { "empty passive effects cannot be suppressed" }
	}

	/** 当前可参与战斗结算的效果。 */
	val activeEffects: List<T>
		get() = if (suppressed) emptyList() else effects

	/** 当前因被动压制而暂不可执行的完整效果。 */
	val suppressedEffects: List<T>
		get() = if (suppressed) effects else emptyList()

	/** 临时关闭非空效果；已关闭或无效果时保持原状态。 */
	fun suppress(): BattlePassiveEffectState<T> =
		if (suppressed || effects.isEmpty()) this else copy(suppressed = true)

	/** 恢复被临时关闭的效果；活动状态保持不变。 */
	fun restore(): BattlePassiveEffectState<T> =
		if (suppressed) copy(suppressed = false) else this
}
