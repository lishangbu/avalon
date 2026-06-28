package io.github.lishangbu.battleengine.model

/**
 * 主要异常状态被阻止附加的原因。
 *
 * 该枚举用于事件流，不直接表达具体技能、特性或道具名称。场地和属性免疫先作为稳定原因暴露；
 * 后续特性免疫、道具免疫和全场效果会继续扩展为新的稳定原因。
 */
enum class BattleStatusBlockReason {
	ELEMENT,
	TERRAIN,
}
