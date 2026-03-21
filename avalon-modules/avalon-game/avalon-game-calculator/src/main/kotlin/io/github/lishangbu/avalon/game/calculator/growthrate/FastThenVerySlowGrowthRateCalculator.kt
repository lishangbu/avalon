package io.github.lishangbu.avalon.game.calculator.growthrate

import org.springframework.stereotype.Service

/**
 * 最慢组成长速率计算器
 *
 * @author lishangbu
 * @since 2026/2/25
 */
@Service
class FastThenVerySlowGrowthRateCalculator : AbstractGrowthRateCalculator() {
    /** 获取最慢组成长速率的内部名称 */
    override fun getGrowthRateInternalName(): String = "fast-then-very-slow"

    /**
     * 最慢组： 分段函数 Lv≤15：EXP = 0.02 * Lv^3 * ⌊ (Lv+73) / 3 ⌋ 16≤Lv≤36：EXP = 0.02 * Lv4 +0.28* Lv^3
     * 37≤Lv≤100：EXP = 0.02 * Lv^3 * ⌊ (Lv + 64) / 2 ⌋
     */
    override fun tryCalculateGrowthRate(level: Int): Int {
        val cubedLevel = level * level * level
        return if (level <= 15) {
            val term = 24 + ((level + 1) / 3)
            cubedLevel * term / 50
        } else if (level <= 35) {
            val term = 14 + level
            cubedLevel * term / 50
        } else {
            val term = 32 + (level / 2)
            cubedLevel * term / 50
        }
    }
}
