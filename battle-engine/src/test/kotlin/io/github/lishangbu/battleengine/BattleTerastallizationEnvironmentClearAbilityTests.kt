package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleTerastallizationEnvironmentClearAbilityTests {
	@Test
	fun `teraform zero clears weather and terrain when its holder terastallizes`() {
		val skill = damagingSkill(skillId = 1090)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant(
						"teraform-zero-user",
						100,
						skill = skill,
						abilityEffects = listOf(BattleAbilityEffect.TerastallizationEnvironmentClear()),
					).copy(teraElementId = 18),
					second = participant("target", 50),
					environment = BattleEnvironment(
						weather = BattleWeather.RAIN,
						terrain = BattleTerrain.ELECTRIC,
					),
				),
			),
			listOf(BattleAction.UseSkill("teraform-zero-user", skill.skillId, "target", terastallize = true)),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(BattleWeather.NONE, resolved.environment.weather)
		assertEquals(BattleTerrain.NONE, resolved.environment.terrain)
	}
}
