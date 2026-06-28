package io.github.lishangbu.battleengine.model

/**
 * 当前战斗场地。
 *
 * 场地影响通常只作用于满足条件的在场成员。第一批只把场地作为环境状态保存，并实现青草场地的回合末回复；
 * 其它场地的状态免疫、优先度封锁和伤害倍率会在独立 fixture 通过后逐步补齐。
 */
enum class BattleTerrain {
	NONE,
	ELECTRIC,
	GRASSY,
	MISTY,
	PSYCHIC,
}
