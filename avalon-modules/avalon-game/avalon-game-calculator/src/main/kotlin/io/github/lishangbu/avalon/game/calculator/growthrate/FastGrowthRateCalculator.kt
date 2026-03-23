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
    /** 获取成长速率内部名称 */
    override fun getGrowthRateInternalName(): String = "fast"

    /** 计算快组在指定等级下的经验值 */
    override fun tryCalculateGrowthRate(level: Int): Int = (4 * level * level * level) / 5
}
