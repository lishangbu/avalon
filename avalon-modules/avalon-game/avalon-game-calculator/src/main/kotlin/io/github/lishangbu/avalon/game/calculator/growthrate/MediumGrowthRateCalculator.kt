package io.github.lishangbu.avalon.game.calculator.growthrate

import org.springframework.stereotype.Service

/**
 * 较快组成长速率计算器
 *
 * @author lishangbu
 * @since 2026/2/25
 */
@Service
class MediumGrowthRateCalculator : AbstractGrowthRateCalculator() {
    /** 获取成长速率内部名称 */
    override fun getGrowthRateInternalName(): String = "medium"

    /** 计算较快组在指定等级下的经验值 */
    override fun tryCalculateGrowthRate(level: Int): Int = level * level * level
}
