package io.github.lishangbu.avalon.game.calculator.growthrate

import org.springframework.stereotype.Service

/**
 * 慢组成长速率计算器
 *
 * @author lishangbu
 * @since 2026/2/25
 */
@Service
class SlowGrowthRateCalculator : AbstractGrowthRateCalculator() {
    /** 获取慢组成长速率的内部名称 */
    override fun getGrowthRateInternalName(): String = "slow"

    /** 慢组： EXP = 1.25 * Lv^3 */
    override fun tryCalculateGrowthRate(level: Int): Int = (5 * level * level * level) / 4
}
