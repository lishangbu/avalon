package io.github.lishangbu.battleengine.model

/**
 * 主动替换请求被拒绝的稳定原因。
 *
 * 枚举值表达规则来源，而不是 Kotlin 实现类名。这样事件流可以在保持可读性的同时承载不同替换限制的共享事实：
 * 成员仍留在原席位、相关倒计时不在替换阶段消费，后续技能阶段仍按原状态继续推进。
 */
enum class SwitchPreventionReason {
	/** 成员仍处于锁招状态，必须继续使用被锁定的技能。 */
	LOCKED_MOVE,

	/** 成员处于休整状态，不能通过非法替换跳过休整。 */
	RECHARGE,

	/** 成员正在蓄力，下一次行动会自动释放蓄力技能。 */
	CHARGING,

	/** 成员被仍在场的来源成员束缚，不能主动离场。 */
	BINDING,
}
