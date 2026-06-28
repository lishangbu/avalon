package io.github.lishangbu.battleengine.model

/**
 * 战斗站位模式。
 *
 * 引擎第一阶段只实现 `SINGLE` 的完整行动结算；`DOUBLE` 先作为格式快照中的显式值存在，
 * 用于后续扩展相邻目标、范围技能和双打同速结算。枚举只表达现代规则下的站位形态，
 * 不携带历史版本或外部平台语义。
 */
enum class BattleMode {
	SINGLE,
	DOUBLE,
}
