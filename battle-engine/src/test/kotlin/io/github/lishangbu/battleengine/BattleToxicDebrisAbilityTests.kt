package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardKind
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleToxicDebrisAbilityTests {
	@Test
	fun `toxic debris adds toxic spikes to the attackers side after physical damage`() {
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100),
					second = participant(
						"holder",
						50,
						abilityEffects = listOf(
							BattleAbilityEffect.ReceivedPhysicalDamageOpponentSideHazard(BattleSideEntryHazardKind.TOXIC_SPIKES),
						),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", 1, "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(
			1,
			resolved.sideOf("attacker")?.entryHazards?.single { it.kind == BattleSideEntryHazardKind.TOXIC_SPIKES }?.layers,
		)
	}
}
