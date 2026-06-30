package io.github.lishangbu.battleengine.model

/**
 * 全场范围内改变行动速度顺序的持续效果。
 *
 * 该模型用于表达戏法空间这类“全场速度排序规则”而不是一侧或单个成员的速度倍率。它不会改变
 * [BattleParticipant] 的面板速度，也不会修改能力阶级、道具或天气带来的有效速度；它只改变同一行动优先度内
 * 有效速度的比较方向。也就是说，优先度仍然先结算，只有优先度相同的行动才会在该效果下低速先动。
 *
 * `turnsRemaining` 的生命周期与天气、场地、一侧屏障保持一致：效果建立的回合已经完成本次行动排序，回合末
 * 会统一递减一次；为空表示测试用例或外部调用方暂不要求引擎管理持续回合。
 */
data class BattleFieldSpeedOrderEffect(
	val kind: BattleFieldSpeedOrderKind,
	val turnsRemaining: Int? = null,
) {
	init {
		require(turnsRemaining == null || turnsRemaining > 0) { "turnsRemaining must be positive when present" }
	}

	/**
	 * 推进一个完整回合后的剩余状态。
	 */
	fun advanceTurn(): BattleFieldSpeedOrderEffect? =
		when (turnsRemaining) {
			null -> this
			1 -> null
			else -> copy(turnsRemaining = turnsRemaining - 1)
		}
}

/**
 * 全场速度顺序效果种类。
 *
 * `reversesSpeedOrder` 明确表达排序方向，避免行动队列直接识别技能 ID 或资料表 effectPolicy。
 */
enum class BattleFieldSpeedOrderKind(
	val reversesSpeedOrder: Boolean,
) {
	TRICK_ROOM(reversesSpeedOrder = true),
}
