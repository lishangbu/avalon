package io.github.lishangbu.battleengine.model

/**
 * 主要异常状态被阻止附加的原因。
 *
 * 该枚举用于事件流，不直接表达具体技能、特性或道具名称。第一批只接入场地阻止睡眠；
 * 后续特性免疫、道具免疫、类型免疫和全场效果会继续扩展为新的稳定原因。
 */
enum class BattleStatusBlockReason {
	TERRAIN,
}
