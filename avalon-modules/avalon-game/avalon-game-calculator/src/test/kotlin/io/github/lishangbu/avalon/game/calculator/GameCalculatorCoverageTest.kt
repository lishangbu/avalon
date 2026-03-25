package io.github.lishangbu.avalon.game.calculator

import io.github.lishangbu.avalon.game.calculator.growthrate.SlowGrowthRateCalculator
import io.github.lishangbu.avalon.game.calculator.stat.HpStatCalculator
import io.github.lishangbu.avalon.game.calculator.stat.NonHpStatCalculator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.ComponentScan

class GameCalculatorCoverageTest {
    @Test
    fun gameCalculatorAutoConfigurationDeclaresExpectedAnnotations() {
        val autoConfiguration = GameCalculatorAutoConfiguration()
        val configurationAnnotation = autoConfiguration::class.java.getAnnotation(AutoConfiguration::class.java)
        val componentScanAnnotation = autoConfiguration::class.java.getAnnotation(ComponentScan::class.java)

        assertNotNull(configurationAnnotation)
        assertEquals(
            listOf("io.github.lishangbu.avalon.game.calculator"),
            componentScanAnnotation.basePackages.toList(),
        )
    }

    @Test
    fun slowGrowthRateCalculatorHandlesBoundaryLevelsDirectly() {
        val calculator = SlowGrowthRateCalculator()

        assertEquals(0, calculator.calculateGrowthRate(0))
        assertEquals(0, calculator.calculateGrowthRate(1))
        assertEquals(10, calculator.calculateGrowthRate(2))
    }

    @Test
    fun hpStatCalculatorReturnsZeroForInvalidLevel() {
        val calculator = HpStatCalculator()

        val result =
            calculator.calculateStat(
                base = 78,
                dv = 31,
                stateExp = 252,
                level = 0,
                nature = 100,
            )

        assertEquals(0, result)
    }

    @Test
    fun nonHpStatCalculatorReturnsZeroForInvalidLevel() {
        val calculator = NonHpStatCalculator()

        val result =
            calculator.calculateStat(
                base = 110,
                dv = 31,
                stateExp = 252,
                level = 0,
                nature = 110,
            )

        assertEquals(0, result)
    }
}
