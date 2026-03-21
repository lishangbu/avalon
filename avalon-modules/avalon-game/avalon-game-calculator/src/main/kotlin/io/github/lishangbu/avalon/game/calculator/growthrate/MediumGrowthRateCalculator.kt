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
    /** 获取较快组成长速率的内部名称 */
    override fun getGrowthRateInternalName(): String = "medium"

    /** 较快组： EXP = Lv^3 */
    override fun tryCalculateGrowthRate(level: Int): Int = level * level * level
}
