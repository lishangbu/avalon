package io.github.lishangbu.avalon.game.calculator.growthrate

import org.springframework.stereotype.Service

/**
 * 较慢组成长速率计算器
 *
 * @author lishangbu
 * @since 2026/2/25
 */
@Service
class MediumSlowGrowthRateCalculator : AbstractGrowthRateCalculator() {
    /** 获取成长速率内部名称 */
    override fun getGrowthRateInternalName(): String = "medium-slow"

    /** 计算较慢组在指定等级下的经验值 */
    override fun tryCalculateGrowthRate(level: Int): Int = (6 * level * level * level) / 5 - 15 * level * level + 100 * level - 140
}
