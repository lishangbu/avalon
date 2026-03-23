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
    /** 获取成长速率内部名称 */
    override fun getGrowthRateInternalName(): String = "fast-then-very-slow"

    /**
     * 计算最慢组在指定等级下的经验值
     *
     * 使用官方分段函数实现
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
