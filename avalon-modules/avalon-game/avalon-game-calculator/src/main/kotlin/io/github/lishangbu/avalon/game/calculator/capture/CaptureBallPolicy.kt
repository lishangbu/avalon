package io.github.lishangbu.avalon.game.calculator.capture

/**
 * 捕捉球修正策略。
 *
 * 该接口只负责把“球 + 上下文”转换成纯数值修正，
 * 不参与 HP、状态、摇晃概率等后续公式计算。
 */
fun interface CaptureBallPolicy {
    /**
     * 解析给定输入对应的球修正结果。
     *
     * @param input 捕捉率计算输入
     * @return 球的数值修正结果
     */
    fun resolve(input: CaptureRateInput): BallResolution
}
