package io.github.lishangbu.battleengine.model

/**
 * 战斗中可以产生阶级变化的能力项。
 *
 * 这些枚举值对应引擎内部语义，不直接暴露数据库表名。`ACCURACY` 和 `EVASION` 的倍率曲线不同于普通能力，
 * 会在命中判定阶段分别修正技能基础命中和目标闪避。
 */
enum class BattleStat {
	ATTACK,
	DEFENSE,
	SPECIAL_ATTACK,
	SPECIAL_DEFENSE,
	SPEED,
	ACCURACY,
	EVASION,
}
