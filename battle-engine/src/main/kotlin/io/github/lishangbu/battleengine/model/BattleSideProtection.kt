package io.github.lishangbu.battleengine.model

/**
 * 一侧场上防护效果。
 *
 * 这类效果和光墙/反射壁一样挂在战斗侧，而不是挂在单个成员身上；但它不参与伤害倍率计算，所以不能复用
 * [BattleSideDamageReduction]。当前覆盖现代规则中两个最稳定的一侧防护：
 * - 白雾防止己方成员被其它成员降低能力阶级。
 * - 神秘守护防止己方成员被其它成员附加主要异常状态或混乱。
 *
 * `turnsRemaining` 采用和其它一侧持续效果一致的回合末递减语义。为空表示测试或外部调用方暂不要求引擎管理
 * 生命周期；正式资料中的白雾和神秘守护都会写入 5 回合。
 */
data class BattleSideProtection(
	val kind: BattleSideProtectionKind,
	val turnsRemaining: Int? = null,
) {
	init {
		require(turnsRemaining == null || turnsRemaining > 0) { "turnsRemaining must be positive when present" }
	}

	/**
	 * 推进一个完整回合后的剩余状态。
	 *
	 * 一侧防护在回合末统一递减；剩余 1 回合表示本回合结束后失效，因此返回 null。这个函数只返回新状态，
	 * 不产生结束事件，保持和既有屏障/顺风生命周期一致。
	 */
	fun advanceTurn(): BattleSideProtection? =
		when (turnsRemaining) {
			null -> this
			1 -> null
			else -> copy(turnsRemaining = turnsRemaining - 1)
		}
}

/**
 * 一侧防护效果种类。
 *
 * 枚举值表达引擎已经显式支持的规则语义，不使用资料表 code。新增防护类型时必须补充对应的技能运行态映射、
 * 阻止入口和公开规则对照测试，避免资料里新增 code 后被静默当成普通无效果技能。
 */
enum class BattleSideProtectionKind {
	STAT_STAGE_REDUCTION,
	STATUS_CONDITION,
}
