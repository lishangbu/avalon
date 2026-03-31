package io.github.lishangbu.avalon.game.calculator.capture

/**
 * 捕捉率计算器。
 *
 * 该接口专注于“数学结果”：
 *
 * - 计算中间值 `a`
 * - 计算单摇阈值
 * - 计算整体成功概率
 *
 * 它不负责：
 *
 * - 生成随机数
 * - 决定本次 battle 实际摇了几次
 * - 扣球、入盒、写日志
 */
fun interface CaptureRateCalculator {
    /**
     * 计算捕捉率相关结果。
     *
     * @param input 捕捉率输入
     * @return 可供 battle 模块复用的纯数值结果
     */
    fun calculate(input: CaptureRateInput): CaptureRateResult
}
