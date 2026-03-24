package io.github.lishangbu.avalon.game.calculator.stat

import org.springframework.stereotype.Service

/**
 * 非 HP 常规能力值计算器
 *
 * 适用于攻击、防御、特攻、特防和速度五项能力。
 *
 * 公式：
 *
 * `Other = floor((floor((2 * base + IV + floor(EV / 4)) * level / 100) + 5) * nature / 100)`
 */
@Service
class NonHpStatCalculator :
    AbstractStatCalculator(
        "attack",
        "defense",
        "special-attack",
        "special-defense",
        "speed",
    ) {
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
        val baseStatValue = calculateBaseStatValue(base, dv, stateExp, level)
        return ((baseStatValue + 5) * nature) / 100
    }
}
