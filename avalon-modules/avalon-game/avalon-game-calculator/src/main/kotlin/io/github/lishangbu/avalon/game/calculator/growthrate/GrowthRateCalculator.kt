package io.github.lishangbu.avalon.game.calculator.growthrate

/**
 * 成长速率计算器
 *
 * 定义不同经验曲线的统一计算契约
 */
interface GrowthRateCalculator {
    /**
     * 计算达到指定等级所需的总经验值
     * @param level 目标等级，必须大于0
     * @return 达到指定等级所需的总经验值，等级无效时返回0
     */
    fun calculateGrowthRate(level: Int): Int

    /**
     * 判断是否支持计算指定成长速率类型
     *
     * @param growthRateInternalName 成长速率内部名称
     * @return 是否支持该成长速率
     */
    fun support(growthRateInternalName: String): Boolean
}
