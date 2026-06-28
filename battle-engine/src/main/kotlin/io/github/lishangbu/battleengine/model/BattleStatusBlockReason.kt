package io.github.lishangbu.battleengine.model

/**
 * 主要异常状态被阻止附加的原因。
 *
 * 该枚举用于事件流，不直接表达具体技能、特性或道具名称。属性、场地、特性和道具只作为稳定来源暴露，
 * 具体资料名称由上层 replay 或资料系统根据成员快照补充。
 */
enum class BattleStatusBlockReason {
	ELEMENT,
	TERRAIN,
	ABILITY,
	ITEM,
}
