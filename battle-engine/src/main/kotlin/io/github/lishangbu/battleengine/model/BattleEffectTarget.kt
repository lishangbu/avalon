package io.github.lishangbu.battleengine.model

/**
 * 技能附加效果的作用对象。
 *
 * 第一批只支持使用者和选中目标。双打范围目标、己方全场、对方全场和全部相邻目标会在双打目标系统中扩展。
 */
enum class BattleEffectTarget {
	USER,
	TARGET,
}
