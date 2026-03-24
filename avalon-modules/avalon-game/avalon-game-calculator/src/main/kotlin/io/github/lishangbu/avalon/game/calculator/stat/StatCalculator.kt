package io.github.lishangbu.avalon.game.calculator.stat

/**
 * 能力值计算器
 *
 * 采用第三世代及之后的常规能力值公式：
 *
 * `HP = floor((2 * base + IV + floor(EV / 4)) * level / 100) + level + 10`
 *
 * `Other = floor((floor((2 * base + IV + floor(EV / 4)) * level / 100) + 5) * nature / 100)`
 *
 * 其中 `nature` 以百分比传入，提升为 `110`，不修正为 `100`，下降为 `90`。
 *
 * @author lishangbu
 * @since 2026/2/26
 */
interface StatCalculator {
    /**
     * 按第三世代及之后的常规公式计算给定等级的能力值
     *
     * `HP` 与其他五项能力的计算公式不同，且 `HP` 不受性格修正影响。
     *
     * @param base 种族值
     * @param dv 个体值（IV）
     * @param stateExp 努力值（EV）
     * @param level 等级
     * @param nature 性格修正百分比，提升为110，普通为100，下降为90；HP 会忽略该值
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
