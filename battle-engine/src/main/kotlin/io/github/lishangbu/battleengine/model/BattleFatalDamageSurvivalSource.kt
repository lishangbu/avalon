package io.github.lishangbu.battleengine.model

/**
 * 满 HP 致命伤害保留 1 HP 的规则来源。
 *
 * 事件流只记录来源类型和来源 ID，不记录具体特性名或道具名，避免把本地化资料文本混入纯引擎状态机。
 */
enum class BattleFatalDamageSurvivalSource {
	/**
	 * 来源于成员当前特性。
	 */
	ABILITY,

	/**
	 * 来源于成员当前携带道具。
	 */
	ITEM,
}
