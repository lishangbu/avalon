package io.github.lishangbu.avalon.dataset.service.impl

import java.math.BigDecimal

/**
 * 属性相克倍率的定点编解码器。
 *
 * 数据库存储的不是浮点数，而是放大 100 倍后的整数百分比：
 * - 100 表示 1.00x
 * - 50 表示 0.50x
 * - 200 表示 2.00x
 *
 * 这样设计有两个目的：
 * 1. 避免 0.5、1.5 这类值在浮点存取和比较时产生二进制误差。
 * 2. 让数据库层、仓储层和业务层都围绕同一套定点规则运转。
 *
 * 双属性计算同样保持在这套定点规则内完成。
 * 假设两个已编码倍率分别为 a、b，它们的真实值是 a/100 和 b/100。
 * 为了让乘法结果继续保持“放大 100 倍后的整数”这一编码约定，需要执行：
 *
 *     ((a / 100) * (b / 100)) * 100 = a * b / 100
 *
 * 例如：
 * - 50(0.50x) * 50(0.50x) / 100 = 25
 * - 25 再解码回真实倍率后就是 0.25x
 */
internal object TypeEffectivenessMultiplierCodec {
    internal const val ONE_X_PERCENT: Int = 100
    private val STORAGE_DIVISOR: BigDecimal = BigDecimal.valueOf(ONE_X_PERCENT.toLong())
    private val ALLOWED_ENTRY_MULTIPLIERS: List<BigDecimal> =
        listOf(
            BigDecimal.ZERO,
            BigDecimal("0.5"),
            BigDecimal.ONE,
            BigDecimal("2"),
        )

    /**
     * 将 API 层的自然倍率编码为数据库使用的定点整数。
     *
     * 这里保留 `BigDecimal` 作为 API 类型，是为了继续向上层暴露自然倍率语义；
     * 但一旦进入持久化边界，必须立即转换为整数百分比，避免后续链路回到浮点。
     */
    internal fun encodeEntryMultiplier(multiplier: BigDecimal?): Int? {
        if (multiplier == null) {
            return null
        }
        require(ALLOWED_ENTRY_MULTIPLIERS.any { it.compareTo(multiplier) == 0 }) {
            "multiplier must be one of ${allowedEntryMultiplierLabels()} or null"
        }
        return multiplier.multiply(STORAGE_DIVISOR).intValueExact()
    }

    /**
     * 将数据库中的定点整数解码为 API 层返回的自然倍率。
     *
     * 该方法只在对外输出时调用，保证数据库内部始终只流转整数定点值。
     */
    internal fun decode(storedPercent: Int?): BigDecimal? = storedPercent?.let { BigDecimal.valueOf(it.toLong()).divide(STORAGE_DIVISOR) }

    /**
     * 在不离开定点表示的前提下完成倍率乘法。
     *
     * `leftPercent` 和 `rightPercent` 都是“放大 100 倍后的整数”，
     * 所以乘积需要再除以 100 才能回到同一缩放单位。
     */
    internal fun multiplyStoredPercents(
        leftPercent: Int,
        rightPercent: Int,
    ): Int {
        val scaledProduct = leftPercent.toLong() * rightPercent.toLong()
        check(scaledProduct % ONE_X_PERCENT.toLong() == 0L) {
            "Stored multiplier product $scaledProduct cannot be represented with scale $ONE_X_PERCENT"
        }
        return (scaledProduct / ONE_X_PERCENT).toInt()
    }

    private fun allowedEntryMultiplierLabels(): String = ALLOWED_ENTRY_MULTIPLIERS.joinToString(prefix = "[", postfix = "]") { it.stripTrailingZeros().toPlainString() }
}
