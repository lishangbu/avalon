package io.github.lishangbu.battleengine.model

/**
 * 技能在当前战斗站位中可影响的目标范围。
 *
 * 该枚举只表达现代规则下的运行时目标集合，不绑定任何资料库技能分类或外部平台命名。
 * 引擎会在技能执行时根据当前上场成员重新计算实际目标；如果范围内只有一个可用目标，
 * 范围伤害修正不会生效。如果范围内存在多个目标，即使后续某个目标因保护、闪避或属性免疫没有扣血，
 * 普通伤害公式仍会使用范围目标倍率。
 *
 * [USER_SIDE_ACTIVE] 专门表达“使用者与同侧当前上场同伴”这类现代双打辅助技能目标。它不包含后备成员，也不会
 * 代表一侧场地；真正的一侧场地效果仍使用 side condition / side protection 模型承载。
 */
enum class BattleSkillTargetScope {
	SELF,
	SELECTED_TARGET,
	USER_SIDE_ACTIVE,
	ALL_ADJACENT_OPPONENTS,
	ALL_ADJACENT_PARTICIPANTS,
	RANDOM_ADJACENT_OPPONENT;

	/**
	 * 当前范围是否可能在双打中同时影响多个成员。
	 */
	val canAffectMultipleTargets: Boolean
		get() = this == ALL_ADJACENT_OPPONENTS || this == ALL_ADJACENT_PARTICIPANTS
}
