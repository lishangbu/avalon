package io.github.lishangbu.avalon.game.calculator.capture

/**
 * 默认捕捉球策略。
 *
 * 这里保持与当前项目 battle capture 设计一致：
 *
 * - 常见球使用倍率修正
 * - `heavy-ball` 使用平坦捕获率修正
 * - `master-ball` 直接成功
 *
 * 这使得 battle 模块后续接入时可以直接复用这一层，而无需再次硬编码球规则。
 */
class DefaultCaptureBallPolicy : CaptureBallPolicy {
    override fun resolve(input: CaptureRateInput): BallResolution =
        when (input.ballItemInternalName.trim().lowercase()) {
            "master-ball" -> {
                BallResolution(directSuccess = true, note = "master-ball")
            }

            "ultra-ball" -> {
                BallResolution(multiplier = 2.0)
            }

            "great-ball" -> {
                BallResolution(multiplier = 1.5)
            }

            "poke-ball" -> {
                BallResolution(multiplier = 1.0)
            }

            "repeat-ball" -> {
                BallResolution(multiplier = if (input.captureContext.alreadyCaught) 3.5 else 1.0)
            }

            "timer-ball" -> {
                BallResolution(multiplier = (1.0 + 0.3 * (input.turn - 1)).coerceAtMost(4.0))
            }

            "beast-ball" -> {
                BallResolution(multiplier = if (input.captureContext.isUltraBeast) 5.0 else 0.1)
            }

            "quick-ball" -> {
                BallResolution(multiplier = if (input.turn == 1) 5.0 else 1.0)
            }

            "dusk-ball" -> {
                BallResolution(multiplier = if (input.captureContext.isNight || input.captureContext.isCave) 3.0 else 1.0)
            }

            "net-ball" -> {
                BallResolution(multiplier = if (hasType(input.captureContext, "water") || hasType(input.captureContext, "bug")) 3.5 else 1.0)
            }

            "dive-ball" -> {
                BallResolution(
                    multiplier =
                        if (input.captureContext.isFishingEncounter || input.captureContext.isSurfEncounter) {
                            3.5
                        } else {
                            1.0
                        },
                )
            }

            "nest-ball" -> {
                BallResolution(multiplier = nestBallMultiplier(input.captureContext.targetLevel))
            }

            "heavy-ball" -> {
                BallResolution(flatCaptureRateBonus = heavyBallFlatBonus(input.captureContext.targetWeight))
            }

            else -> {
                throw IllegalArgumentException("Unsupported capture ball '${input.ballItemInternalName}'.")
            }
        }

    /**
     * `nest-ball` 在目标等级低于 30 时有更高倍率。
     *
     * 这里直接沿用项目现有策略：
     * `max(10, 41 - level) / 10`
     */
    private fun nestBallMultiplier(targetLevel: Int?): Double {
        if (targetLevel == null || targetLevel >= 30) {
            return 1.0
        }
        return ((41 - targetLevel).coerceAtLeast(10)) / 10.0
    }

    /**
     * `heavy-ball` 使用平坦捕获率修正，而不是倍率修正。
     *
     * 体重区间逻辑保持与 battle capture 文档一致。
     */
    private fun heavyBallFlatBonus(targetWeight: Int?): Int =
        when {
            targetWeight == null || targetWeight <= 0 -> 0
            targetWeight < 1024 -> -20
            targetWeight < 2048 -> 0
            targetWeight < 3072 -> 20
            else -> 30
        }

    private fun hasType(
        context: CaptureContext,
        typeInternalName: String,
    ): Boolean = context.targetTypes.any { type -> type.equals(typeInternalName, ignoreCase = true) }
}
