package io.github.lishangbu.avalon.game.calculator.capture

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.data.Offset.offset
import org.junit.jupiter.api.Test

class DefaultCaptureRateCalculatorTest {
    private val calculator = DefaultCaptureRateCalculator()

    @Test
    fun masterBall_shouldReturnGuaranteedSuccessWithoutShakeCheck() {
        val result =
            calculator.calculate(
                CaptureRateInput(
                    currentHp = 30,
                    maxHp = 120,
                    captureRate = 45,
                    statusId = "par",
                    ballItemInternalName = "master-ball",
                ),
            )

        assertThat(result.directSuccess).isTrue()
        assertThat(result.guaranteedSuccess).isTrue()
        assertThat(result.shakeCheckThreshold).isNull()
        assertThat(result.overallCaptureSuccessProbability).isEqualTo(1.0)
        assertThat(result.overallCaptureSuccessRate).isEqualTo(100.0)
        assertThat(result.note).isEqualTo("master-ball")
    }

    @Test
    fun pokeBall_shouldMatchKnownBaselineFormulaValues() {
        val result =
            calculator.calculate(
                CaptureRateInput(
                    currentHp = 30,
                    maxHp = 120,
                    captureRate = 45,
                    statusId = "par",
                    ballItemInternalName = "poke-ball",
                    turn = 5,
                ),
            )

        assertThat(result.directSuccess).isFalse()
        assertThat(result.guaranteedSuccess).isFalse()
        assertThat(result.effectiveCaptureRate).isEqualTo(45)
        assertThat(result.ballMultiplier).isEqualTo(1.0)
        assertThat(result.statusMultiplier).isEqualTo(1.5)
        assertThat(result.hpFactor).isCloseTo(0.8333333333333334, offset(1.0E-12))
        assertThat(result.captureValue).isCloseTo(56.25, offset(1.0E-12))
        assertThat(result.normalizedCaptureValueRate).isCloseTo(22.058823529411764, offset(1.0E-12))
        assertThat(result.shakeCheckThreshold).isNotNull
        assertThat(result.shakeCheckThreshold!!).isCloseTo(44912.67168345236, offset(1.0E-9))
        assertThat(result.singleShakeSuccessProbability).isCloseTo(0.6853179931640625, offset(1.0E-15))
        assertThat(result.overallCaptureSuccessProbability).isCloseTo(0.22058122173852507, offset(1.0E-15))
        assertThat(result.overallCaptureSuccessRate).isCloseTo(22.058122173852507, offset(1.0E-12))
    }

    @Test
    fun strongerBallModifiers_shouldIncreaseOverallProbability() {
        val pokeBallResult =
            calculator.calculate(
                CaptureRateInput(
                    currentHp = 30,
                    maxHp = 120,
                    captureRate = 45,
                    statusId = "par",
                    ballItemInternalName = "poke-ball",
                ),
            )
        val greatBallResult =
            calculator.calculate(
                CaptureRateInput(
                    currentHp = 30,
                    maxHp = 120,
                    captureRate = 45,
                    statusId = "par",
                    ballItemInternalName = "great-ball",
                ),
            )
        val ultraBallResult =
            calculator.calculate(
                CaptureRateInput(
                    currentHp = 30,
                    maxHp = 120,
                    captureRate = 45,
                    statusId = "par",
                    ballItemInternalName = "ultra-ball",
                ),
            )

        assertThat(greatBallResult.captureValue).isGreaterThan(pokeBallResult.captureValue)
        assertThat(greatBallResult.overallCaptureSuccessProbability).isGreaterThan(pokeBallResult.overallCaptureSuccessProbability)
        assertThat(ultraBallResult.captureValue).isGreaterThan(greatBallResult.captureValue)
        assertThat(ultraBallResult.overallCaptureSuccessProbability).isGreaterThan(greatBallResult.overallCaptureSuccessProbability)
    }

    @Test
    fun contextualBalls_shouldApplyExpectedContextModifiers() {
        val repeatCaughtResult =
            calculator.calculate(
                CaptureRateInput(
                    currentHp = 30,
                    maxHp = 120,
                    captureRate = 45,
                    ballItemInternalName = "repeat-ball",
                    captureContext = CaptureContext(alreadyCaught = true),
                ),
            )
        val repeatUncaughtResult =
            calculator.calculate(
                CaptureRateInput(
                    currentHp = 30,
                    maxHp = 120,
                    captureRate = 45,
                    ballItemInternalName = "repeat-ball",
                    captureContext = CaptureContext(alreadyCaught = false),
                ),
            )
        val duskBallResult =
            calculator.calculate(
                CaptureRateInput(
                    currentHp = 30,
                    maxHp = 120,
                    captureRate = 45,
                    ballItemInternalName = "dusk-ball",
                    captureContext = CaptureContext(isNight = true),
                ),
            )
        val quickBallResult =
            calculator.calculate(
                CaptureRateInput(
                    currentHp = 30,
                    maxHp = 120,
                    captureRate = 45,
                    ballItemInternalName = "quick-ball",
                    turn = 1,
                ),
            )

        assertThat(repeatCaughtResult.ballMultiplier).isEqualTo(3.5)
        assertThat(repeatCaughtResult.overallCaptureSuccessProbability).isGreaterThan(repeatUncaughtResult.overallCaptureSuccessProbability)
        assertThat(duskBallResult.ballMultiplier).isEqualTo(3.0)
        assertThat(quickBallResult.ballMultiplier).isEqualTo(5.0)
    }

    @Test
    fun nestBallAndHeavyBall_shouldUseLevelAndWeightAsNumericInputs() {
        val lowLevelNestResult =
            calculator.calculate(
                CaptureRateInput(
                    currentHp = 30,
                    maxHp = 120,
                    captureRate = 45,
                    ballItemInternalName = "nest-ball",
                    captureContext = CaptureContext(targetLevel = 5),
                ),
            )
        val neutralNestResult =
            calculator.calculate(
                CaptureRateInput(
                    currentHp = 30,
                    maxHp = 120,
                    captureRate = 45,
                    ballItemInternalName = "nest-ball",
                    captureContext = CaptureContext(targetLevel = 30),
                ),
            )
        val heavyTargetResult =
            calculator.calculate(
                CaptureRateInput(
                    currentHp = 30,
                    maxHp = 120,
                    captureRate = 45,
                    ballItemInternalName = "heavy-ball",
                    captureContext = CaptureContext(targetWeight = 3200),
                ),
            )
        val lightTargetResult =
            calculator.calculate(
                CaptureRateInput(
                    currentHp = 30,
                    maxHp = 120,
                    captureRate = 45,
                    ballItemInternalName = "heavy-ball",
                    captureContext = CaptureContext(targetWeight = 900),
                ),
            )

        assertThat(lowLevelNestResult.ballMultiplier).isEqualTo(3.6)
        assertThat(lowLevelNestResult.overallCaptureSuccessProbability).isGreaterThan(neutralNestResult.overallCaptureSuccessProbability)
        assertThat(heavyTargetResult.flatCaptureRateBonus).isEqualTo(30)
        assertThat(lightTargetResult.flatCaptureRateBonus).isEqualTo(-20)
        assertThat(heavyTargetResult.overallCaptureSuccessProbability).isGreaterThan(lightTargetResult.overallCaptureSuccessProbability)
    }

    @Test
    fun statusAliases_shouldResolveToExpectedMultipliers() {
        val sleepResult =
            calculator.calculate(
                CaptureRateInput(
                    currentHp = 30,
                    maxHp = 120,
                    captureRate = 45,
                    statusId = "sleep",
                    ballItemInternalName = "poke-ball",
                ),
            )
        val toxicResult =
            calculator.calculate(
                CaptureRateInput(
                    currentHp = 30,
                    maxHp = 120,
                    captureRate = 45,
                    statusId = "tox",
                    ballItemInternalName = "poke-ball",
                ),
            )
        val noStatusResult =
            calculator.calculate(
                CaptureRateInput(
                    currentHp = 30,
                    maxHp = 120,
                    captureRate = 45,
                    statusId = null,
                    ballItemInternalName = "poke-ball",
                ),
            )

        assertThat(sleepResult.statusMultiplier).isEqualTo(2.0)
        assertThat(toxicResult.statusMultiplier).isEqualTo(1.5)
        assertThat(noStatusResult.statusMultiplier).isEqualTo(1.0)
        assertThat(sleepResult.overallCaptureSuccessProbability).isGreaterThan(toxicResult.overallCaptureSuccessProbability)
        assertThat(toxicResult.overallCaptureSuccessProbability).isGreaterThan(noStatusResult.overallCaptureSuccessProbability)
    }

    @Test
    fun invalidInput_shouldFailFast() {
        assertThatThrownBy {
            calculator.calculate(
                CaptureRateInput(
                    currentHp = 121,
                    maxHp = 120,
                    captureRate = 45,
                    ballItemInternalName = "poke-ball",
                ),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("currentHp")

        assertThatThrownBy {
            calculator.calculate(
                CaptureRateInput(
                    currentHp = 30,
                    maxHp = 120,
                    captureRate = 45,
                    ballItemInternalName = "unknown-ball",
                ),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unsupported capture ball")
    }
}
