package io.github.lishangbu.avalon.game.calculator.stat

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StatCalculatorFactoryTest {
    private val statCalculatorFactory =
        StatCalculatorFactory(
            listOf(
                HpStatCalculator(),
                NonHpStatCalculator(),
            ),
        )

    private companion object {
        /** 满个体值 */
        const val MAX_IV = 31

        /** 满努力值 */
        const val MAX_EV = 252

        /** 对战常用 50 级 */
        const val LEVEL_50 = 50

        /** 满级 100 级 */
        const val LEVEL_100 = 100

        /** 性格下降修正 */
        const val HINDERED_NATURE = 90

        /** 性格不修正 */
        const val NEUTRAL_NATURE = 100

        /** 性格提升修正 */
        const val BOOSTED_NATURE = 110
    }

    /** 喷火龙在 50 级、31 IV、252 EV、无性格修正时的 HP 应为 185 */
    @Test
    fun calculateStat_shouldCalculateCharizardHpAtLevel50() {
        val result =
            calculateStat(
                internalName = "hp",
                base = 78,
                level = LEVEL_50,
            )

        assertThat(result).isEqualTo(185)
    }

    /** 固执卡比兽在 50 级满个体满努力时，攻击应被 1.1 倍性格修正到 178 */
    @Test
    fun calculateStat_shouldCalculateAdamantSnorlaxAttackAtLevel50() {
        val result =
            calculateStat(
                internalName = "attack",
                base = 110,
                level = LEVEL_50,
                nature = BOOSTED_NATURE,
            )

        assertThat(result).isEqualTo(178)
    }

    /** 内敛超梦的攻击会被性格压低，50 级满个体满努力时应为 145 */
    @Test
    fun calculateStat_shouldCalculateModestMewtwoAttackAtLevel50() {
        val result =
            calculateStat(
                internalName = "attack",
                base = 110,
                level = LEVEL_50,
                nature = HINDERED_NATURE,
            )

        assertThat(result).isEqualTo(145)
    }

    /** 内敛喷火龙的特攻会被性格提升，50 级满个体满努力时应为 177 */
    @Test
    fun calculateStat_shouldCalculateModestCharizardSpecialAttackAtLevel50() {
        val result =
            calculateStat(
                internalName = "special-attack",
                base = 109,
                level = LEVEL_50,
                nature = BOOSTED_NATURE,
            )

        assertThat(result).isEqualTo(177)
    }

    /** 胆小皮卡丘的速度会被性格提升，50 级满个体满努力时应为 156 */
    @Test
    fun calculateStat_shouldCalculateTimidPikachuSpeedAtLevel50() {
        val result =
            calculateStat(
                internalName = "speed",
                base = 90,
                level = LEVEL_50,
                nature = BOOSTED_NATURE,
            )

        assertThat(result).isEqualTo(156)
    }

    /** HP 不受性格影响，因此卡比兽 HP 在提升性格下也应保持同样结果 */
    @Test
    fun calculateStat_shouldIgnoreNatureForSnorlaxHp() {
        val result =
            calculateStat(
                internalName = "hp",
                base = 160,
                level = LEVEL_50,
                nature = BOOSTED_NATURE,
            )

        assertThat(result).isEqualTo(267)
    }

    /** 脱壳忍者是特例，任意常规参数下 HP 都固定为 1 */
    @Test
    fun calculateStat_shouldReturnOneForShedinjaHpAtLevel100() {
        val result =
            calculateStat(
                internalName = "hp",
                base = 1,
                level = LEVEL_100,
            )

        assertThat(result).isEqualTo(1)
    }

    /** 能力内部名称匹配应忽略大小写，例如 Dragonite 的 SPEED 也能正常计算 */
    @Test
    fun calculateStat_shouldSupportCaseInsensitiveDragoniteSpeedStatName() {
        val result =
            calculateStat(
                internalName = "SPEED",
                base = 80,
                level = LEVEL_50,
            )

        assertThat(result).isEqualTo(132)
    }

    /** 非法等级不参与计算，应直接返回 0 */
    @Test
    fun calculateStat_shouldReturnZeroForInvalidLevel() {
        val result =
            calculateStat(
                internalName = "attack",
                base = 110,
                level = 0,
                nature = BOOSTED_NATURE,
            )

        assertThat(result).isZero()
    }

    /** 当前只支持六围能力值，命中等战斗阶段能力不参与此处计算 */
    @Test
    fun calculateStat_shouldReturnZeroForUnsupportedStat() {
        val result =
            calculateStat(
                internalName = "accuracy",
                base = 0,
            )

        assertThat(result).isZero()
    }

    /** 使用统一默认参数构造测试样例，减少重复样板代码 */
    private fun calculateStat(
        internalName: String,
        base: Int,
        dv: Int = MAX_IV,
        stateExp: Int = MAX_EV,
        level: Int = LEVEL_50,
        nature: Int = NEUTRAL_NATURE,
    ): Int =
        statCalculatorFactory.calculateStat(
            internalName = internalName,
            base = base,
            dv = dv,
            stateExp = stateExp,
            level = level,
            nature = nature,
        )
}
