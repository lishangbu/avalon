package io.github.lishangbu.battleengine.model

import kotlin.math.floor

/**
 * 能力阶级倍率工具。
 *
 * 普通能力项使用现代主系列常见的 `正阶级=(2+n)/2`、`负阶级=2/(2-n)` 曲线。
 * 命中和闪避使用 `3` 分母曲线，分别在命中判定中修正使用者命中阶级和目标闪避阶级。
 * 这里提供纯函数，避免把倍率算法散落在伤害、速度排序和命中代码里。
 */
class BattleStatStageModifiers {
	/**
	 * 按普通能力阶级修正基础数值，结果至少为 1。
	 */
	fun modifiedBattleStat(base: Int, stage: Int): Int {
		require(base > 0) { "base stat must be positive" }
		require(stage in -6..6) { "stage must be between -6 and 6" }
		val multiplier = if (stage >= 0) {
			(2.0 + stage) / 2.0
		} else {
			2.0 / (2.0 - stage)
		}
		return floor(base * multiplier).toInt().coerceAtLeast(1)
	}

	/**
	 * 按命中/闪避阶级修正倍率。
	 *
	 * 正阶级提升命中方有效命中或闪避方有效闪避，负阶级按相反方向缩小倍率。调用方负责决定该倍率
	 * 是作为分子使用还是作为目标闪避的分母使用；引擎命中判定会用 `技能命中 * 使用者命中倍率 / 目标闪避倍率`。
	 */
	fun accuracyMultiplier(stage: Int): Double {
		require(stage in -6..6) { "stage must be between -6 and 6" }
		return if (stage >= 0) {
			(3.0 + stage) / 3.0
		} else {
			3.0 / (3.0 - stage)
		}
	}
}
