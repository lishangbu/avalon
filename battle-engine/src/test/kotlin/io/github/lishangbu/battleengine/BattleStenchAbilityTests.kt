package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertTrue

class BattleStenchAbilityTests {
	@Test
	fun `stench adds a flinch chance to damaging skills without their own flinch effect`() {
		val skill = damagingSkill(skillId = 931, power = 40)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant(
						"attacker",
						100,
						skill = skill,
						abilityEffects = listOf(BattleAbilityEffect.AdditionalFlinchChance(10)),
					),
					second = participant("defender", 50),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "defender")),
			ScriptedBattleRandom(listOf(1, 15, 0)),
		)

		assertTrue(
			resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplied>()
				.any { it.status == BattleVolatileStatus.FLINCH },
		)
	}
}
