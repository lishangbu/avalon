package io.github.lishangbu.battleengine.model

/**
 * 战斗中可以产生阶级变化的能力项。
 *
 * 这些枚举值对应引擎内部语义，不直接暴露数据库表名。`ACCURACY` 和 `EVASION` 的倍率曲线不同于普通能力，
 * 第一批只保存阶级变化事件，命中/闪避修正会在命中公式扩展批次中消费它们。
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
