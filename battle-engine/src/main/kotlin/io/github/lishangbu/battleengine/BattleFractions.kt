package io.github.lishangbu.battleengine

/**
 * 战斗中常见比例 HP 数值的整数结算。
 *
 * 这些函数只负责现代规则中“按比例得到一个整数 HP 数值”的基础取整，不读取战斗状态、不追加事件，也不判断
 * 回复封锁、免疫、替身或倒下。调用方仍负责根据当前 HP、最大 HP、伤害来源和规则阶段夹取最终结果。
 */
internal fun fractionAmount(value: Int, numerator: Int, denominator: Int): Int {
	if (value <= 0) {
		return 0
	}
	return ((value.toLong() * numerator) / denominator)
		.coerceIn(1, Int.MAX_VALUE.toLong())
		.toInt()
}

/**
 * 计算四舍五入到最近整数的比例 HP 数值。
 *
 * 技能反作用伤害和普通比例回复的取整口径不同，所以单独保留该函数。正比例在调用方确认已经造成实际伤害后至少
 * 为 1 点，保证低伤害命中仍能产生可观察的反作用伤害。
 */
internal fun roundedHalfUpFractionAmount(value: Int, numerator: Int, denominator: Int): Int {
	if (value <= 0) {
		return 0
	}
	return ((value.toLong() * numerator * 2 + denominator) / (denominator * 2L))
		.coerceIn(1, Int.MAX_VALUE.toLong())
		.toInt()
}
