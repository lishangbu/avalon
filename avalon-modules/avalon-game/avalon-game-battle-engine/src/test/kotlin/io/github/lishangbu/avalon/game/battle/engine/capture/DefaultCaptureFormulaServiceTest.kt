package io.github.lishangbu.avalon.game.battle.engine.capture

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultCaptureFormulaServiceTest {
    @Test
    fun finalRate_shouldExposeOverallCaptureSuccessRate() {
        val service = DefaultCaptureFormulaService(captureRandomSource = FixedCaptureRandomSource(0, 0, 0, 0))

        val result =
            service.calculate(
                captureInput(
                    ballItemInternalName = "poke-ball",
                    currentHp = 30,
                    maxHp = 120,
                    captureRate = 45,
                    statusId = "par",
                ),
            )

        assertEquals(56.25, result.captureValue)
        assertEquals(22.058122173852507, result.finalRate, 1.0e-12)
    }

    @Test
    fun masterBall_shouldAlwaysSucceed() {
        val service = DefaultCaptureFormulaService(captureRandomSource = FixedCaptureRandomSource(65535, 65535, 65535, 65535))

        val result =
            service.calculate(
                captureInput(ballItemInternalName = "master-ball"),
            )

        assertTrue(result.success)
        assertEquals(4, result.shakes)
        assertEquals("master-ball", result.reason)
        assertEquals(100.0, result.finalRate)
    }

    @Test
    fun greatBall_shouldProduceHigherCaptureValueThanPokeBall() {
        val service = DefaultCaptureFormulaService(captureRandomSource = FixedCaptureRandomSource(0, 0, 0, 0))

        val pokeBallResult = service.calculate(captureInput(ballItemInternalName = "poke-ball"))
        val greatBallResult = service.calculate(captureInput(ballItemInternalName = "great-ball"))

        assertTrue(greatBallResult.captureValue > pokeBallResult.captureValue)
        assertTrue(greatBallResult.finalRate > pokeBallResult.finalRate)
    }

    @Test
    fun repeatBall_shouldUseCaughtMultiplier() {
        val service = DefaultCaptureFormulaService(captureRandomSource = FixedCaptureRandomSource(0, 0, 0, 0))

        val uncaughtResult =
            service.calculate(
                captureInput(
                    ballItemInternalName = "repeat-ball",
                    battleContext = CaptureContext(alreadyCaught = false),
                ),
            )
        val caughtResult =
            service.calculate(
                captureInput(
                    ballItemInternalName = "repeat-ball",
                    battleContext = CaptureContext(alreadyCaught = true),
                ),
            )

        assertTrue(caughtResult.captureValue > uncaughtResult.captureValue)
        assertTrue(caughtResult.finalRate > uncaughtResult.finalRate)
    }

    @Test
    fun timerBall_shouldScaleWithTurnCount() {
        val service = DefaultCaptureFormulaService(captureRandomSource = FixedCaptureRandomSource(0, 0, 0, 0))

        val earlyTurnResult = service.calculate(captureInput(ballItemInternalName = "timer-ball", turn = 1))
        val lateTurnResult = service.calculate(captureInput(ballItemInternalName = "timer-ball", turn = 10))

        assertTrue(lateTurnResult.captureValue > earlyTurnResult.captureValue)
        assertTrue(lateTurnResult.finalRate > earlyTurnResult.finalRate)
    }

    @Test
    fun quickBall_shouldOnlyBoostOnFirstTurn() {
        val service = DefaultCaptureFormulaService(captureRandomSource = FixedCaptureRandomSource(0, 0, 0, 0))

        val firstTurnResult = service.calculate(captureInput(ballItemInternalName = "quick-ball", turn = 1))
        val laterTurnResult = service.calculate(captureInput(ballItemInternalName = "quick-ball", turn = 2))

        assertTrue(firstTurnResult.captureValue > laterTurnResult.captureValue)
        assertTrue(firstTurnResult.finalRate > laterTurnResult.finalRate)
    }

    @Test
    fun environmentAndContextBalls_shouldUseProvidedContext() {
        val service = DefaultCaptureFormulaService(captureRandomSource = FixedCaptureRandomSource(0, 0, 0, 0))

        val duskBallResult =
            service.calculate(
                captureInput(
                    ballItemInternalName = "dusk-ball",
                    battleContext = CaptureContext(isNight = true),
                ),
            )
        val diveBallResult =
            service.calculate(
                captureInput(
                    ballItemInternalName = "dive-ball",
                    battleContext = CaptureContext(isSurfEncounter = true),
                ),
            )
        val netBallResult =
            service.calculate(
                captureInput(
                    ballItemInternalName = "net-ball",
                    battleContext = CaptureContext(targetTypes = setOf("water")),
                ),
            )

        assertTrue(duskBallResult.captureValue > captureInputResult(service, "poke-ball"))
        assertTrue(diveBallResult.captureValue > captureInputResult(service, "poke-ball"))
        assertTrue(netBallResult.captureValue > captureInputResult(service, "poke-ball"))
    }

    @Test
    fun nestAndHeavyBall_shouldUseLevelAndWeight() {
        val service = DefaultCaptureFormulaService(captureRandomSource = FixedCaptureRandomSource(0, 0, 0, 0))

        val lowLevelNestResult =
            service.calculate(
                captureInput(
                    ballItemInternalName = "nest-ball",
                    battleContext = CaptureContext(targetLevel = 5),
                ),
            )
        val neutralNestResult =
            service.calculate(
                captureInput(
                    ballItemInternalName = "nest-ball",
                    battleContext = CaptureContext(targetLevel = 30),
                ),
            )
        val heavyTargetResult =
            service.calculate(
                captureInput(
                    ballItemInternalName = "heavy-ball",
                    battleContext = CaptureContext(targetWeight = 3200),
                ),
            )
        val lightTargetResult =
            service.calculate(
                captureInput(
                    ballItemInternalName = "heavy-ball",
                    battleContext = CaptureContext(targetWeight = 900),
                ),
            )

        assertTrue(lowLevelNestResult.captureValue > neutralNestResult.captureValue)
        assertTrue(lowLevelNestResult.finalRate > neutralNestResult.finalRate)
        assertTrue(heavyTargetResult.captureValue > lightTargetResult.captureValue)
        assertTrue(heavyTargetResult.finalRate > lightTargetResult.finalRate)
    }

    @Test
    fun fixedRandomSource_shouldMakeShakeOutcomePredictable() {
        val successService = DefaultCaptureFormulaService(captureRandomSource = FixedCaptureRandomSource(0, 0, 0, 0))
        val failureService = DefaultCaptureFormulaService(captureRandomSource = FixedCaptureRandomSource(65535, 65535, 65535, 65535))
        val input =
            captureInput(
                ballItemInternalName = "poke-ball",
                currentHp = 30,
                maxHp = 120,
                captureRate = 45,
                statusId = "par",
            )

        val successResult = successService.calculate(input)
        val failureResult = failureService.calculate(input)

        assertTrue(successResult.success)
        assertEquals(4, successResult.shakes)
        assertEquals("all-shakes-passed", successResult.reason)
        assertFalse(failureResult.success)
        assertEquals(0, failureResult.shakes)
        assertEquals("failed-first-shake", failureResult.reason)
    }

    private fun captureInput(
        ballItemInternalName: String,
        currentHp: Int = 30,
        maxHp: Int = 120,
        captureRate: Int = 45,
        statusId: String? = "par",
        turn: Int = 5,
        battleContext: CaptureContext = CaptureContext(),
    ): CaptureFormulaInput =
        CaptureFormulaInput(
            currentHp = currentHp,
            maxHp = maxHp,
            captureRate = captureRate,
            statusId = statusId,
            ballItemInternalName = ballItemInternalName,
            turn = turn,
            battleContext = battleContext,
        )

    private fun captureInputResult(
        service: DefaultCaptureFormulaService,
        ballItemInternalName: String,
    ): Double = service.calculate(captureInput(ballItemInternalName = ballItemInternalName)).captureValue

    private class FixedCaptureRandomSource(
        vararg rolls: Int,
    ) : CaptureRandomSource {
        private val values: List<Int> = rolls.toList()
        private var nextIndex: Int = 0

        override fun nextShakeRoll(): Int =
            values
                .getOrElse(nextIndex++) { values.lastOrNull() ?: 0 }
    }
}
