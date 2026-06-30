package io.github.lishangbu.battleengine.model

/**
 * 当前战斗场地。
 *
 * 场地影响通常只作用于满足条件的在场成员。当前实现覆盖青草场地回合末回复、电气场地阻止接地成员新获得睡眠，
 * 以及薄雾场地阻止接地成员新获得主要异常状态。其它场地的优先度封锁和伤害倍率会在独立测试用例通过后逐步补齐。
 */
enum class BattleTerrain {
	NONE,
	ELECTRIC,
	GRASSY,
	MISTY,
	PSYCHIC,
}
