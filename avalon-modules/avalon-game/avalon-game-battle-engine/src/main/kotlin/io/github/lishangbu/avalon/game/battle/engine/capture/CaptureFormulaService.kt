package io.github.lishangbu.avalon.game.battle.engine.capture

interface CaptureFormulaService {
    fun calculate(
        input: CaptureFormulaInput,
        nextShakeRoll: (() -> Int)? = null,
    ): CaptureFormulaResult
}
