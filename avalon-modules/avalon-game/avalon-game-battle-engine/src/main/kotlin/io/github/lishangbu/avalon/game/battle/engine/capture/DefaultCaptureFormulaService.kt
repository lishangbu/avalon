package io.github.lishangbu.avalon.game.battle.engine.capture

import io.github.lishangbu.avalon.game.calculator.capture.CaptureRateCalculator
import io.github.lishangbu.avalon.game.calculator.capture.CaptureRateInput
import io.github.lishangbu.avalon.game.calculator.capture.DefaultCaptureRateCalculator
import io.github.lishangbu.avalon.game.calculator.capture.CaptureContext as CalculatorCaptureContext

/**
 * battle-engine 捕捉公式服务。
 *
 * 数值部分统一委托给 `avalon-game-calculator`，battle-engine 只保留实际摇晃判定，
 * 这样可以保证：
 *
 * - 公式与概率口径只有一份实现
 * - battle 层仍然可以继续使用自己的随机源推进真实捕捉流程
 */
class DefaultCaptureFormulaService(
    private val captureRandomSource: CaptureRandomSource = DefaultCaptureRandomSource(),
    private val captureRateCalculator: CaptureRateCalculator = DefaultCaptureRateCalculator(),
) : CaptureFormulaService {
    override fun calculate(
        input: CaptureFormulaInput,
        nextShakeRoll: (() -> Int)?,
    ): CaptureFormulaResult {
        val captureRateResult = captureRateCalculator.calculate(input.toCaptureRateInput())
        val shakeRollSupplier = nextShakeRoll ?: { captureRandomSource.nextShakeRoll() }

        if (captureRateResult.directSuccess || captureRateResult.guaranteedSuccess) {
            return CaptureFormulaResult(
                success = true,
                shakes = 4,
                captureValue = captureRateResult.captureValue,
                finalRate = captureRateResult.overallCaptureSuccessRate,
                ballRate = captureRateResult.ballMultiplier,
                statusRate = captureRateResult.statusMultiplier,
                reason = captureRateResult.note,
            )
        }

        if (captureRateResult.captureValue <= 0.0) {
            return CaptureFormulaResult(
                success = false,
                shakes = 0,
                captureValue = 0.0,
                finalRate = 0.0,
                ballRate = captureRateResult.ballMultiplier,
                statusRate = captureRateResult.statusMultiplier,
                reason = "failed-first-shake",
            )
        }

        val threshold =
            requireNotNull(captureRateResult.shakeCheckThreshold) {
                "shakeCheckThreshold must be present when capture is not guaranteed."
            }

        repeat(4) { shakeIndex ->
            val roll = shakeRollSupplier().toDouble()
            if (roll >= threshold) {
                return CaptureFormulaResult(
                    success = false,
                    shakes = shakeIndex,
                    captureValue = captureRateResult.captureValue,
                    finalRate = captureRateResult.overallCaptureSuccessRate,
                    ballRate = captureRateResult.ballMultiplier,
                    statusRate = captureRateResult.statusMultiplier,
                    reason = failureReason(shakeIndex),
                )
            }
        }

        return CaptureFormulaResult(
            success = true,
            shakes = 4,
            captureValue = captureRateResult.captureValue,
            finalRate = captureRateResult.overallCaptureSuccessRate,
            ballRate = captureRateResult.ballMultiplier,
            statusRate = captureRateResult.statusMultiplier,
            reason = "all-shakes-passed",
        )
    }

    private fun CaptureFormulaInput.toCaptureRateInput(): CaptureRateInput =
        CaptureRateInput(
            currentHp = currentHp,
            maxHp = maxHp,
            captureRate = captureRate,
            statusId = statusId,
            ballItemInternalName = ballItemInternalName,
            turn = turn,
            captureContext =
                CalculatorCaptureContext(
                    alreadyCaught = battleContext.alreadyCaught,
                    isFishingEncounter = battleContext.isFishingEncounter,
                    isSurfEncounter = battleContext.isSurfEncounter,
                    isNight = battleContext.isNight,
                    isCave = battleContext.isCave,
                    isUltraBeast = battleContext.isUltraBeast,
                    targetLevel = battleContext.targetLevel,
                    targetWeight = battleContext.targetWeight,
                    targetTypes = battleContext.targetTypes,
                ),
        )

    private fun failureReason(shakeIndex: Int): String =
        when (shakeIndex) {
            0 -> "failed-first-shake"
            1 -> "failed-second-shake"
            2 -> "failed-third-shake"
            else -> "failed-fourth-shake"
        }
}
