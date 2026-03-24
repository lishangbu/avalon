package io.github.lishangbu.avalon.game.calculator.stat

/**
 * 抽象能力值计算器
 *
 * 提供属性支持判断和第三世代后的公共中间值计算
 */
abstract class AbstractStatCalculator(
    private vararg val supportedStats: String,
) : StatCalculator {
    /** 判断是否支持指定能力项 */
    override fun support(stateInternalName: String): Boolean = supportedStats.any { supportedStat -> supportedStat.equals(stateInternalName, ignoreCase = true) }

    /** 计算第三世代之后公式的公共部分 */
    protected fun calculateBaseStatValue(
        base: Int,
        dv: Int,
        stateExp: Int,
        level: Int,
    ): Int = ((2 * base + dv + stateExp / 4) * level) / 100
}
