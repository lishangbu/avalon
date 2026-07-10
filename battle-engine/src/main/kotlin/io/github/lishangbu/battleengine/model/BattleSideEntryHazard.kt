package io.github.lishangbu.battleengine.model

/**
 * 一侧场上的入场陷阱状态。
 *
 * 入场陷阱是放置在某一战斗侧上的持续规则对象，后续属于该侧的新成员换入时才会触发。它不同于反射壁、
 * 光墙等防守屏障：屏障在伤害公式阶段读取，而入场陷阱在换入事件之后立即结算伤害、主要异常状态或能力阶级
 * 变化。因此这里使用独立模型，避免调用方把“场上效果”这个资料概念直接泄漏进引擎核心。
 *
 * `layers` 表示当前层数；只有撒菱和毒菱允许叠层。`maxLayers` 来自现代主系列规则或资料适配层显式传入的
 * 限制，上限必须不超过该陷阱种类的现代规则上限。重复使用不可叠层的陷阱，或已经达到最大层数的可叠层陷阱，
 * 都不会改变战斗状态，也不会刷新为新的持续状态。
 */
data class BattleSideEntryHazard(
	val kind: BattleSideEntryHazardKind,
	val layers: Int = 1,
	val maxLayers: Int = kind.defaultMaxLayers,
) {
	init {
		require(maxLayers in 1..kind.defaultMaxLayers) {
			"maxLayers must be between 1 and the modern rule limit for $kind"
		}
		require(layers in 1..maxLayers) { "layers must be between 1 and maxLayers" }
	}

	/**
	 * 返回增加一层后的陷阱。
	 *
	 * 若当前层数已经达到 [maxLayers]，返回 null 表示状态没有变化。引擎据此决定是否产生层数变化事件；
	 * 这里不记录技能使用者，因为状态对象只描述一侧场上的最终事实。
	 */
	fun addLayer(): BattleSideEntryHazard? =
		if (layers >= maxLayers) {
			null
		} else {
			copy(layers = layers + 1)
		}
}
