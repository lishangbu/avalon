package io.github.lishangbu.battleengine.model

/**
 * 会随上场状态或短期回合流程消失的临时状态。
 *
 * 临时状态不同于主要异常状态：它们可以和主要异常状态共存，通常在成员离场时清除，并且多数只影响
 * 行动前流程。这里列出的枚举只包含需要被通用状态系统统一计时、清理或免疫判断的效果；锁招、替身、蓄力等
 * 虽然也会跨回合存在，但它们拥有更复杂的专属字段和事件顺序，因此保存在成员快照的独立结构中。
 */
enum class BattleVolatileStatus {
	FLINCH,
	CONFUSION,
	HEAL_BLOCK,
	TAUNT,
	DISABLE,
	TORMENT,
	INFATUATION,
	BINDING,
}
