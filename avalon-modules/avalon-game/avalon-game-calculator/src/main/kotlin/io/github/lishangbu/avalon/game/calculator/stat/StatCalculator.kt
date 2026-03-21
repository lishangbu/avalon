package io.github.lishangbu.avalon.game.calculator.stat

/**
 * 能力值计算器
 *
 * @author lishangbu
 * @since 2026/2/26
 */
interface StatCalculator {
    /**
     * 计算给定等级的能力值
     *
     * @param base 种族值
     * @param dv 个体值
     * @param stateExp 努力值
     * @param level 等级
     * @param nature 性格修正
     * @return 计算得到的能力值
     */
    fun calculateStat(
        base: Int,
        dv: Int,
        stateExp: Int,
        level: Int,
        nature: Int,
    ): Int

    /**
     * 判断是否支持计算指定属性
     *
     * @param stateInternalName 属性的内部名称，例如"hp"、"attack"、“defense”等
     * @return 如果支持计算指定属性，返回true；否则返回false
     */
    fun support(stateInternalName: String): Boolean
}
