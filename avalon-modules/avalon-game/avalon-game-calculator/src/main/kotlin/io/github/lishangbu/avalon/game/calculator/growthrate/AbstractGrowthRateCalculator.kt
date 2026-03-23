package io.github.lishangbu.avalon.game.calculator.growthrate

/**
 * 抽象成长速率计算器
 *
 * 提供等级边界处理和内部名称匹配的通用实现
 */
abstract class AbstractGrowthRateCalculator : GrowthRateCalculator {
    /** 判断是否支持指定成长速率 */
    override fun support(growthRateInternalName: String): Boolean = getGrowthRateInternalName().equals(growthRateInternalName, ignoreCase = true)

    /** 计算指定等级的经验值 */
    override fun calculateGrowthRate(level: Int): Int {
        if (level <= 0) return 0
        return if (level == 1) {
            0
        } else {
            tryCalculateGrowthRate(level)
        }
    }

    /** 计算具体成长曲线在指定等级下的经验值 */
    protected abstract fun tryCalculateGrowthRate(level: Int): Int

    /** 获取成长速率内部名称 */
    protected abstract fun getGrowthRateInternalName(): String
}
