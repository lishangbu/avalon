package io.github.lishangbu.avalon.game.battle.engine.capture

import kotlin.random.Random

class DefaultCaptureRandomSource : CaptureRandomSource {
    override fun nextShakeRoll(): Int = Random.nextInt(0, 65536)
}
