package io.github.lishangbu.battleengine.model

import kotlin.math.floor

/**
 * 能力阶级倍率工具。
 *
 * 普通能力项使用现代主系列常见的 `正阶级=(2+n)/2`、`负阶级=2/(2-n)` 曲线。
 * 命中和闪避的 3 分母曲线会在命中系统扩展时使用。这里提供纯函数，避免把倍率算法散落在伤害和排序代码里。
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
	 * 按命中/闪避阶级修正倍率。第一批只暴露公式，暂不接入命中判定。
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
