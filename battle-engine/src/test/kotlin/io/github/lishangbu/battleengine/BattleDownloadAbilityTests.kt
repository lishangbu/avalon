package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleStat
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleDownloadAbilityTests {
	@Test
	fun `download raises attack against lower defense and special attack on ties`() {
		val ability = listOf(BattleAbilityEffect.SwitchInOpponentDefenseComparisonBoost())
		val lowDefenseTarget = participant("target-a", 50).copy(defense = 80, specialDefense = 120)
		val attackState = BattleEngine().start(
			initialState(first = participant("download-a", 100, abilityEffects = ability), second = lowDefenseTarget),
		)
		val tieTarget = participant("target-b", 50).copy(defense = 100, specialDefense = 100)
		val specialAttackState = BattleEngine().start(
			initialState(first = participant("download-b", 100, abilityEffects = ability), second = tieTarget),
		)

		assertEquals(1, attackState.participant("download-a")?.statStage(BattleStat.ATTACK))
		assertEquals(1, specialAttackState.participant("download-b")?.statStage(BattleStat.SPECIAL_ATTACK))
	}
}
