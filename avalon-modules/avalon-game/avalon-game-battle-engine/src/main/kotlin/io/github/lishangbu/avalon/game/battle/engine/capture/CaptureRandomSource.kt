package io.github.lishangbu.avalon.game.battle.engine.capture

fun interface CaptureRandomSource {
    fun nextShakeRoll(): Int
}
