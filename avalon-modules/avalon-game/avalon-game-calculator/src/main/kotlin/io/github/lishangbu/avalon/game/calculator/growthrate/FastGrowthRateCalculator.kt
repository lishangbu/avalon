package io.github.lishangbu.avalon.game.calculator.growthrate

import org.springframework.stereotype.Service

/**
 * 快组成长速率计算器
 *
 * @author lishangbu
 * @since 2026/2/25
 */
@Service
class FastGrowthRateCalculator : AbstractGrowthRateCalculator() {
    /** 获取快组成长速率的内部名称 */
    override fun getGrowthRateInternalName(): String = "fast"

    /** 快组： EXP = 0.8 * Lv^3 */
    override fun tryCalculateGrowthRate(level: Int): Int = (4 * level * level * level) / 5
}
