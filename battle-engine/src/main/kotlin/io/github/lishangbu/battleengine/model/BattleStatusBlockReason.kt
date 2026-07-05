package io.github.lishangbu.battleengine.model

/**
 * 状态被阻止附加的原因。
 *
 * 该枚举用于事件流，同时服务主要异常状态、临时状态和能力阶级下降阻止事件，不直接表达具体技能、特性或道具名称。
 * 属性、场地、替身、特性、道具和一侧防护只作为稳定来源暴露，具体资料名称由上层 replay 或资料系统根据成员快照补充。
 */
enum class BattleStatusBlockReason {
	EXISTING_STATUS,
	ELEMENT,
	TERRAIN,
	SUBSTITUTE,
	ABILITY,
	ITEM,
	SIDE_PROTECTION,
	NO_ELIGIBLE_SKILL,
}
