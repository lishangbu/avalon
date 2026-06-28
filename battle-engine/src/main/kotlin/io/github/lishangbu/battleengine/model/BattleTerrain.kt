package io.github.lishangbu.battleengine.model

/**
 * 当前战斗场地。
 *
 * 场地影响通常只作用于满足条件的在场成员。当前实现覆盖青草场地回合末回复，以及在尚未建模“是否接地”前，
 * 把当前上场成员视作可被电气场地影响并阻止其新获得睡眠。其它场地的状态免疫、优先度封锁和伤害倍率会在独立 fixture 通过后逐步补齐。
 */
enum class BattleTerrain {
	NONE,
	ELECTRIC,
	GRASSY,
	MISTY,
	PSYCHIC,
}
