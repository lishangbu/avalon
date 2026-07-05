package io.github.lishangbu.battleengine.model

/**
 * 致命伤害保留 HP 的规则来源。
 *
 * 特性和携带道具来源只在目标满 HP 时触发；技能来源用于挺住这类当回合行动，它不要求满 HP，也不消耗外部资源。
 * 事件流只记录来源类型和来源 ID，不记录具体特性名、道具名或技能名，避免把本地化资料文本混入纯引擎状态机。
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

	/**
	 * 来源于目标本回合已经成功建立的挺住类技能状态。
	 */
	SKILL,
}
