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
    /** 获取成长速率内部名称 */
    override fun getGrowthRateInternalName(): String = "slow-then-very-fast"

    /**
     * 计算最快组在指定等级下的经验值
     *
     * 使用官方分段函数实现
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
