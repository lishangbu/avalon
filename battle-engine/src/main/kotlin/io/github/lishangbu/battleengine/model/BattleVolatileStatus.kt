package io.github.lishangbu.battleengine.model

/**
 * 会随上场状态或短期回合流程消失的临时状态。
 *
 * 临时状态不同于主要异常状态：它们可以和主要异常状态共存，通常在成员离场时清除，并且多数只影响
 * 行动前流程。第一批接入畏缩、混乱和回复封锁；锁招、寄生、束缚、替身等会在各自规则 fixture 通过后继续扩展。
 */
enum class BattleVolatileStatus {
	FLINCH,
	CONFUSION,
	HEAL_BLOCK,
}
