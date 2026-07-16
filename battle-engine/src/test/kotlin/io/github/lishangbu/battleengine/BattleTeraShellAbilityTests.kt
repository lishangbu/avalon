package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleTeraShellAbilityTests {
	@Test
	fun `tera shell overrides effectiveness while holder is at full hp`() {
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100),
					second = participant(
						"holder",
						50,
						abilityEffects = listOf(BattleAbilityEffect.FullHpEffectivenessOverride(0.5)),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", 1, "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(0.5, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().effectiveness)
	}
}
