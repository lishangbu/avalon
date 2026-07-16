package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleConsumedBerryRestoreAbilityTests {
	@Test
	fun `harvest restores a consumed berry after its end turn roll succeeds`() {
		val skill = damagingSkill(skillId = 1094, power = 40)
		val engine = BattleEngine()
		val berryEffects = listOf(
			BattleItemEffect.BerryMarker(),
			BattleItemEffect.LowHpHeal(healDenominator = 4),
		)
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill),
					second = participant(
						"harvest-holder",
						50,
						currentHp = 60,
						abilityEffects = listOf(
							BattleAbilityEffect.EndTurnConsumedBerryRestore(50, BattleWeather.SUN),
						),
						itemId = 1094,
						itemEffects = berryEffects,
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "harvest-holder")),
			ScriptedBattleRandom(listOf(1, 15, 0)),
		)

		assertEquals(1094, resolved.participant("harvest-holder")?.itemId)
		assertEquals(berryEffects, resolved.participant("harvest-holder")?.itemEffects)
	}
}
