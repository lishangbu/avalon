package io.github.lishangbu.battleengine.model

/**
 * 技能行动在行动前被阻止的稳定原因。
 *
 * 枚举值和公开规则行为一一对应，但多个原因共用 [BattleEvent.SkillPrevented] 事件结构。调用方通过原因字段
 * 判断展示文案和 replay 分支，通过事件上的可选字段读取对应规则需要的技能、临时状态或剩余回合事实。
 */
enum class SkillPreventionReason {
	/** 特性直接阻止本次技能行动。 */
	ABILITY,

	/** 睡眠阻止行动，并会消费一次睡眠阻止计数。 */
	SLEEP,

	/** 冰冻自然解冻失败，本次行动被阻止。 */
	FREEZE,

	/** 麻痹随机判定触发，本次行动被阻止。 */
	PARALYSIS,

	/** 上次成功使用的休整技能要求本次行动空过。 */
	RECHARGE,

	/** 回复封锁阻止回复类技能或吸取回复类技能。 */
	HEAL_BLOCK,

	/** 挑衅阻止变化分类技能。 */
	TAUNT,

	/** 定身法阻止被指定禁用的技能。 */
	DISABLE,

	/** 无理取闹阻止连续使用上一次成功使用的技能。 */
	TORMENT,

	/** 畏缩、混乱等临时状态阻止本次行动。 */
	VOLATILE_STATUS,
}
