package io.github.lishangbu.avalon.game.calculator.growthrate

import org.springframework.stereotype.Service

/**
 * 最快组成长速率计算器
 *
 * @author lishangbu
 * @since 2026/2/25
 */
@Service
class SlowThenVeryFastGrowthRateCalculator : AbstractGrowthRateCalculator() {
    /** 获取最快组成长速率的内部名称 */
    override fun getGrowthRateInternalName(): String = "slow-then-very-fast"

    /**
     * 最快组： 分段函数 Lv≤50：EXP = -0.02 * Lv4 + 2 * Lv^3 51≤Lv≤68：EXP = -0.01 * Lv4 + 1.5 * Lv^3
     * 69≤Lv≤98：EXP = 0.002 * Lv^3 * ⌊ (1911 - 10 * Lv) / 3 ⌋ 99≤Lv≤100：EXP = -0.01 * Lv4 + 1.6 *
     * Lv^3
     */
    override fun tryCalculateGrowthRate(level: Int): Int {
        val cubedLevel = level * level * level
        return if (level <= 50) {
            cubedLevel * (100 - level) / 50
        } else if (level <= 68) {
            cubedLevel * (150 - level) / 100
        } else if (level <= 98) {
            val mod = level % 3
            val term = 1274 + mod * mod - 9 * mod - 20 * (level / 3)
            cubedLevel * term / 1000
        } else {
            cubedLevel * (160 - level) / 100
        }
    }
}
