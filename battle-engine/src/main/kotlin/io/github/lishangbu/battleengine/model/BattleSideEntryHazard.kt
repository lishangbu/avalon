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

/**
 * 入场陷阱的现代规则种类。
 *
 * 这些名称是中性领域概念，不绑定任何品牌术语或数据库文本。具体技能、展示名称和本地化文本由上层资料维护；
 * 引擎只关心现代主系列规则中稳定的行为：
 * - [STEALTH_ROCK] 换入时按岩属性克制倍率造成最大 HP 比例伤害。
 * - [SPIKES] 对接地换入成员造成随层数增加的最大 HP 比例伤害。
 * - [TOXIC_SPIKES] 对接地换入成员附加中毒或剧毒，接地毒属性成员会吸收并移除该陷阱。
 * - [STICKY_WEB] 对接地换入成员降低速度能力阶级。
 */
enum class BattleSideEntryHazardKind(
	val defaultMaxLayers: Int,
) {
	STEALTH_ROCK(defaultMaxLayers = 1),
	SPIKES(defaultMaxLayers = 3),
	TOXIC_SPIKES(defaultMaxLayers = 2),
	STICKY_WEB(defaultMaxLayers = 1),
}

/**
 * 一侧新增或叠加入场陷阱后的状态变更结果。
 *
 * [side] 是已经写入陷阱的下一版战斗侧，[hazard] 是最终生效的陷阱快照。引擎事件层使用该结果记录当前层数，
 * 从而区分“新建第一层”和“叠加到第二/第三层”，同时避免在未变化时产生误导性事件。
 */
data class BattleSideEntryHazardAddResult(
	val side: BattleSide,
	val hazard: BattleSideEntryHazard,
)

/**
 * 战斗状态级别的一侧入场陷阱变更结果。
 *
 * 这是 [BattleState] 写入一侧状态时的返回值：调用方需要新的 [state] 继续结算，同时需要 [hazard] 生成事件。
 * 该类型让不可变状态更新和事件构造分离，避免 `BattleState` 自身理解技能、回合或事件文案。
 */
data class BattleSideEntryHazardStateChange(
	val state: BattleState,
	val hazard: BattleSideEntryHazard,
)
