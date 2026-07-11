package io.github.lishangbu.battlesession

import io.github.lishangbu.battleengine.random.BattleRandom
import io.github.lishangbu.battleengine.random.SecureBattleRandom

fun interface BattleRandomFactory {
	fun create(): BattleRandom
}

internal class SecureBattleRandomFactory : BattleRandomFactory {
	override fun create(): BattleRandom = SecureBattleRandom()
}
