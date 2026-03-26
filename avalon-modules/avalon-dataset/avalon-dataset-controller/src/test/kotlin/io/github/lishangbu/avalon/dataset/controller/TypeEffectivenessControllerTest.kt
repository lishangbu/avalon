package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessCell
import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessChart
import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessCompleteness
import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessMatchup
import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessMatrixCellInput
import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessResult
import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessRow
import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessService
import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessTypeView
import io.github.lishangbu.avalon.dataset.service.UpsertTypeEffectivenessMatrixCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TypeEffectivenessControllerTest {
    @Test
    fun calculateDelegatesToService() {
        val service = FakeTypeEffectivenessService()
        val controller = TypeEffectivenessController(service)
        val expected =
            TypeEffectivenessResult(
                attackingType = TypeEffectivenessTypeView("fire", "火"),
                defendingTypes =
                    listOf(
                        TypeEffectivenessMatchup(
                            defendingType = TypeEffectivenessTypeView("grass", "草"),
                            multiplier = BigDecimal("2"),
                            status = "configured",
                        ),
                    ),
                finalMultiplier = BigDecimal("2"),
                status = "complete",
                effectiveness = "super-effective",
            )
        service.result = expected

        val actual = controller.calculate("fire", listOf("grass"))

        assertSame(expected, actual)
        assertEquals("fire", service.attackingType)
        assertEquals(listOf("grass"), service.defendingTypes)
    }

    @Test
    fun getChartDelegatesToService() {
        val service = FakeTypeEffectivenessService()
        val controller = TypeEffectivenessController(service)
        val expected = chart()
        service.chartResult = expected

        val actual = controller.getChart()

        assertSame(expected, actual)
    }

    @Test
    fun upsertMatrixDelegatesToService() {
        val service = FakeTypeEffectivenessService()
        val controller = TypeEffectivenessController(service)
        val command =
            UpsertTypeEffectivenessMatrixCommand(
                cells = listOf(TypeEffectivenessMatrixCellInput("fire", "grass", BigDecimal("2"))),
            )
        val expected = chart()
        service.chartResult = expected

        val actual = controller.upsertMatrix(command)

        assertSame(expected, actual)
        assertSame(command, service.command)
    }

    private fun chart(): TypeEffectivenessChart =
        TypeEffectivenessChart(
            supportedTypes = listOf(TypeEffectivenessTypeView("fire", "火")),
            completeness = TypeEffectivenessCompleteness(1, 1, 0),
            rows =
                listOf(
                    TypeEffectivenessRow(
                        attackingType = TypeEffectivenessTypeView("fire", "火"),
                        cells =
                            listOf(
                                TypeEffectivenessCell(
                                    defendingType = TypeEffectivenessTypeView("grass", "草"),
                                    multiplier = BigDecimal("2"),
                                    status = "configured",
                                ),
                            ),
                    ),
                ),
        )

    private class FakeTypeEffectivenessService : TypeEffectivenessService {
        var attackingType: String? = null
        var defendingTypes: List<String> = emptyList()
        var command: UpsertTypeEffectivenessMatrixCommand? = null
        var result: TypeEffectivenessResult =
            TypeEffectivenessResult(
                attackingType = TypeEffectivenessTypeView("normal", "一般"),
                defendingTypes = emptyList(),
                finalMultiplier = BigDecimal.ONE,
                status = "complete",
                effectiveness = "normal-effective",
            )
        var chartResult: TypeEffectivenessChart =
            TypeEffectivenessChart(
                supportedTypes = emptyList(),
                completeness = TypeEffectivenessCompleteness(0, 0, 0),
                rows = emptyList(),
            )

        override fun calculate(
            attackingType: String,
            defendingTypes: List<String>,
        ): TypeEffectivenessResult {
            this.attackingType = attackingType
            this.defendingTypes = defendingTypes
            return result
        }

        override fun getChart(): TypeEffectivenessChart = chartResult

        override fun upsertMatrix(command: UpsertTypeEffectivenessMatrixCommand): TypeEffectivenessChart {
            this.command = command
            return chartResult
        }
    }
}
