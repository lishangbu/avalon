package io.github.lishangbu.avalon.game.calculator.capture

import org.springframework.stereotype.Service
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 默认捕捉率计算器。
 *
 * 公式说明：
 *
 * 1. 先解析球修正
 * 2. 计算状态倍率
 * 3. 计算有效捕获率 `max(1, captureRate + flatBonus)`
 * 4. 计算 HP 因子 `((3 * maxHp) - (2 * currentHp)) / (3 * maxHp)`
 * 5. 计算捕捉值 `a = effectiveCaptureRate * hpFactor * ballMultiplier * statusMultiplier`
 * 6. 若 `a >= 255` 则必定成功
 * 7. 否则计算四摇阈值 `b = 1048560 / sqrt(sqrt(16711680 / a))`
 * 8. 将单摇概率提升为四摇全过的整体成功概率
 *
 * 注意：
 *
 * - 这里返回的是“理论成功概率”
 * - battle 层若需要实际摇晃结果，可以在后续用 [CaptureRateResult.shakeCheckThreshold] 配合随机源再做判定
 */
@Service
class DefaultCaptureRateCalculator(
    private val captureBallPolicy: CaptureBallPolicy = DefaultCaptureBallPolicy(),
) : CaptureRateCalculator {
    override fun calculate(input: CaptureRateInput): CaptureRateResult {
        validateInput(input)

        val ballResolution = captureBallPolicy.resolve(input)
        val statusMultiplier = resolveStatusMultiplier(input.statusId)
        val effectiveCaptureRate = (input.captureRate + ballResolution.flatCaptureRateBonus).coerceAtLeast(MIN_CAPTURE_RATE)
        val hpFactor = ((3.0 * input.maxHp) - (2.0 * input.currentHp)) / (3.0 * input.maxHp)

        if (ballResolution.directSuccess) {
            return CaptureRateResult(
                directSuccess = true,
                guaranteedSuccess = true,
                effectiveCaptureRate = effectiveCaptureRate,
                ballMultiplier = ballResolution.multiplier,
                flatCaptureRateBonus = ballResolution.flatCaptureRateBonus,
                statusMultiplier = statusMultiplier,
                hpFactor = hpFactor,
                captureValue = MAX_CAPTURE_VALUE,
                normalizedCaptureValueRate = 100.0,
                shakeCheckThreshold = null,
                singleShakeSuccessProbability = 1.0,
                overallCaptureSuccessProbability = 1.0,
                overallCaptureSuccessRate = 100.0,
                note = ballResolution.note ?: "direct-success-ball",
            )
        }

        val captureValue = effectiveCaptureRate * hpFactor * ballResolution.multiplier * statusMultiplier
        val normalizedCaptureValueRate = (captureValue.coerceIn(0.0, MAX_CAPTURE_VALUE) / MAX_CAPTURE_VALUE) * 100.0

        if (captureValue >= MAX_CAPTURE_VALUE) {
            return CaptureRateResult(
                directSuccess = false,
                guaranteedSuccess = true,
                effectiveCaptureRate = effectiveCaptureRate,
                ballMultiplier = ballResolution.multiplier,
                flatCaptureRateBonus = ballResolution.flatCaptureRateBonus,
                statusMultiplier = statusMultiplier,
                hpFactor = hpFactor,
                captureValue = captureValue,
                normalizedCaptureValueRate = 100.0,
                shakeCheckThreshold = null,
                singleShakeSuccessProbability = 1.0,
                overallCaptureSuccessProbability = 1.0,
                overallCaptureSuccessRate = 100.0,
                note = "auto-success-threshold",
            )
        }

        if (captureValue <= 0.0) {
            return CaptureRateResult(
                directSuccess = false,
                guaranteedSuccess = false,
                effectiveCaptureRate = effectiveCaptureRate,
                ballMultiplier = ballResolution.multiplier,
                flatCaptureRateBonus = ballResolution.flatCaptureRateBonus,
                statusMultiplier = statusMultiplier,
                hpFactor = hpFactor,
                captureValue = 0.0,
                normalizedCaptureValueRate = 0.0,
                shakeCheckThreshold = 0.0,
                singleShakeSuccessProbability = 0.0,
                overallCaptureSuccessProbability = 0.0,
                overallCaptureSuccessRate = 0.0,
                note = "non-positive-capture-value",
            )
        }

        val shakeCheckThreshold = SHAKE_NUMERATOR / sqrt(sqrt(SHAKE_DENOMINATOR / captureValue))
        val singleShakeSuccessProbability = calculateSingleShakeSuccessProbability(shakeCheckThreshold)
        val overallCaptureSuccessProbability = singleShakeSuccessProbability.pow(SHAKE_COUNT)

        return CaptureRateResult(
            directSuccess = false,
            guaranteedSuccess = false,
            effectiveCaptureRate = effectiveCaptureRate,
            ballMultiplier = ballResolution.multiplier,
            flatCaptureRateBonus = ballResolution.flatCaptureRateBonus,
            statusMultiplier = statusMultiplier,
            hpFactor = hpFactor,
            captureValue = captureValue,
            normalizedCaptureValueRate = normalizedCaptureValueRate,
            shakeCheckThreshold = shakeCheckThreshold,
            singleShakeSuccessProbability = singleShakeSuccessProbability,
            overallCaptureSuccessProbability = overallCaptureSuccessProbability,
            overallCaptureSuccessRate = overallCaptureSuccessProbability * 100.0,
            note = "standard-four-shake",
        )
    }

    private fun validateInput(input: CaptureRateInput) {
        require(input.maxHp > 0) { "maxHp must be greater than 0." }
        require(input.currentHp in 0..input.maxHp) { "currentHp must be between 0 and maxHp." }
        require(input.captureRate >= 0) { "captureRate must be greater than or equal to 0." }
        require(input.turn > 0) { "turn must be greater than 0." }
    }

    /**
     * 状态倍率。
     *
     * 与常见主系列规则一致：
     *
     * - 睡眠、冰冻：2.0
     * - 麻痹、灼伤、中毒：1.5
     * - 其他或无状态：1.0
     */
    private fun resolveStatusMultiplier(statusId: String?): Double =
        when (statusId?.trim()?.lowercase()) {
            "slp", "sleep", "frz", "freeze", "frozen" -> 2.0
            "par", "paralysis", "paralyzed", "brn", "burn", "psn", "poison", "tox", "toxic" -> 1.5
            else -> 1.0
        }

    /**
     * 根据离散整数随机空间精确计算“单次摇晃成功概率”。
     *
     * battle 中 shake roll 的取值空间是 `0..65535`，共 65536 个整数。
     * 判定条件为：`roll < threshold`。
     *
     * 因此，成功样本数不是简单的 `threshold`，而是：
     *
     * - `threshold <= 0` 时为 0
     * - `threshold >= 65536` 时为 65536
     * - 否则为 `ceil(threshold)`
     */
    private fun calculateSingleShakeSuccessProbability(threshold: Double): Double {
        if (threshold <= 0.0) {
            return 0.0
        }
        if (threshold >= SHAKE_ROLL_SPACE) {
            return 1.0
        }
        val successfulRollCount = ceil(threshold).toInt().coerceIn(0, SHAKE_ROLL_SPACE)
        return successfulRollCount.toDouble() / SHAKE_ROLL_SPACE
    }

    private companion object {
        private const val MIN_CAPTURE_RATE = 1
        private const val MAX_CAPTURE_VALUE = 255.0
        private const val SHAKE_ROLL_SPACE = 65536
        private const val SHAKE_COUNT = 4.0
        private const val SHAKE_NUMERATOR = 1048560.0
        private const val SHAKE_DENOMINATOR = 16711680.0
    }
}
