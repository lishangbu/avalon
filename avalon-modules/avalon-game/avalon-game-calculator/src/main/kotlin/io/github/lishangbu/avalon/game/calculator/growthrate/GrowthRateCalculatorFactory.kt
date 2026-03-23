package io.github.lishangbu.avalon.game.calculator.growthrate

import org.springframework.stereotype.Service

/**
 * 成长速率计算工厂
 *
 * 根据内部名称选择对应的成长速率计算器
 */
@Service
class GrowthRateCalculatorFactory(
    /** 计算器列表 */
    private val calculators: List<GrowthRateCalculator>,
) {
    /**
     * 计算达到指定等级所需的总经验值
     * @param internalName 成长速率内部名称
     * @param level 目标等级，必须大于0
     * @return 达到指定等级所需的总经验值，等级无效时返回0
     */
    fun calculateGrowthRate(
        internalName: String,
        level: Int,
    ): Int {
        if (level <= 0) return 0
        if (level == 1) {
            return 0
        }
        return calculators
            .firstOrNull { calculator -> calculator.support(internalName) }
            ?.calculateGrowthRate(level) ?: 0
    }
}
