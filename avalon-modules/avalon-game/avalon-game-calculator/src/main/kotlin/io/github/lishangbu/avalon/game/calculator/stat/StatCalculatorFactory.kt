package io.github.lishangbu.avalon.game.calculator.stat

import org.springframework.stereotype.Service

/**
 * 能力值计算工厂
 *
 * 根据能力内部名称选择对应的能力值计算器。
 */
@Service
class StatCalculatorFactory(
    /** 计算器列表 */
    private val calculators: List<StatCalculator>,
) {
    /**
     * 按第三世代后的公式计算指定能力项的能力值
     *
     * @param internalName 能力内部名称，例如 hp、attack、special-attack
     * @param base 种族值
     * @param dv 个体值（IV）
     * @param stateExp 努力值（EV）
     * @param level 等级
     * @param nature 性格修正百分比，提升为110，普通为100，下降为90
     * @return 计算结果；能力项未知或等级无效时返回0
     */
    fun calculateStat(
        internalName: String,
        base: Int,
        dv: Int,
        stateExp: Int,
        level: Int,
        nature: Int,
    ): Int {
        if (level <= 0) {
            return 0
        }
        return calculators
            .firstOrNull { calculator -> calculator.support(internalName) }
            ?.calculateStat(base, dv, stateExp, level, nature) ?: 0
    }
}
