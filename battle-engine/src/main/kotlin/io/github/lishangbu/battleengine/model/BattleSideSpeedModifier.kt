package io.github.lishangbu.battleengine.model

/**
 * 一侧场上影响行动速度结算的持续效果。
 *
 * 该模型只表达“某一侧当前存在一个速度倍率”，不保存技能名称、资料表 code 或 UI 文案。现代主系列规则中，
 * 顺风会让己方一侧成员在行动排序时按 2 倍速度参与比较；它不是能力阶级变化，也不会直接修改成员面板速度，
 * 因此必须放在一侧场上状态里，而不是写回 [BattleParticipant]。
 *
 * `turnsRemaining` 表示该效果还会经历多少个回合末递减。技能建立效果的当回合已经完成行动排序，所以效果通常
 * 从下一次排序开始可见；回合末统一递减能让它与防守屏障、天气、场地使用一致的生命周期语义。
 */
data class BattleSideSpeedModifier(
	val kind: BattleSideSpeedModifierKind,
	val multiplier: Double = kind.defaultMultiplier,
	val turnsRemaining: Int? = null,
) {
	init {
		require(multiplier > 0.0 && !multiplier.isNaN() && !multiplier.isInfinite()) {
			"multiplier must be a finite positive number"
		}
		require(turnsRemaining == null || turnsRemaining > 0) { "turnsRemaining must be positive when present" }
	}

	/**
	 * 推进一个完整回合后的剩余状态。
	 *
	 * 空持续回合表示测试用例或外部调用方暂不要求引擎管理生命周期；剩余 1 回合的状态会在本次回合末移除。
	 */
	fun advanceTurn(): BattleSideSpeedModifier? =
		when (turnsRemaining) {
			null -> this
			1 -> null
			else -> copy(turnsRemaining = turnsRemaining - 1)
		}
}

/**
 * 一侧速度修正种类。
 *
 * 每个枚举值都对应一个已经显式接入引擎的现代规则效果。新增速度类场上效果时应扩展这里并补充公开对照测试，
 * 不应在行动排序中解析资料表字符串。
 */
enum class BattleSideSpeedModifierKind(
	val defaultMultiplier: Double,
) {
	TAILWIND(2.0),
}
