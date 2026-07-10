package io.github.lishangbu.battleengine.model

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
