package io.github.lishangbu.avalon.game.calculator.stat

import org.springframework.stereotype.Service

/**
 * HP 能力值计算器
 *
 * 公式：
 *
 * `HP = floor((2 * base + IV + floor(EV / 4)) * level / 100) + level + 10`
 *
 * 特例：
 *
 * 脱壳忍者的 HP 固定为 `1`。
 */
@Service
class HpStatCalculator : AbstractStatCalculator("hp") {
    override fun calculateStat(
        base: Int,
        dv: Int,
        stateExp: Int,
        level: Int,
        nature: Int,
    ): Int {
        if (level <= 0) {
            return 0
        }
        // 计算器目前只接收能力项的内部名，不接收物种信息。
        // 现有资料中只有脱壳忍者的 HP 种族值为 1，因此这里按 base=1 处理固定 HP。
        if (base == 1) {
            return 1
        }
        return calculateBaseStatValue(base, dv, stateExp, level) + level + 10
    }
}
