package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleThresholdSwitchAbilityTests {
	@Test
	fun `crossing half hp forces holder to a legal reserve`() {
		val skill = damagingSkill(skillId = 941, power = null, fixedDamage = BattleFixedDamage.FixedAmount(60))
		val holder = participant(
			"holder",
			50,
			abilityEffects = listOf(BattleAbilityEffect.DamageCrossedHpThresholdForceSelfSwitch()),
		)
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(
					initialState(
						first = participant("attacker", 100, skill = skill),
						second = holder,
						secondBench = listOf(participant("reserve", 40)),
					),
				),
				listOf(BattleAction.UseSkill("attacker", skill.skillId, "holder")),
				ScriptedBattleRandom(emptyList()),
			)
		}

		assertEquals(listOf("reserve"), resolved.sideOf("reserve")?.activeActorIds)
		assertTrue(resolved.events.any { it is BattleEvent.AbilityForcedSwitchSelected })
	}
}
