package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleLowHpItemThresholdAbilityTests {
	@Test
	fun `gluttony activates a quarter hp berry at half hp`() {
		val skill = damagingSkill(skillId = 1092, power = 40)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill),
					second = participant(
						"gluttony-holder",
						50,
						currentHp = 68,
						abilityEffects = listOf(BattleAbilityEffect.LowHpItemTriggerThresholdHalf()),
						itemId = 1092,
						itemEffects = listOf(BattleItemEffect.LowHpStatStageBoost(BattleStat.ATTACK, 1)),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "gluttony-holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(40, resolved.participant("gluttony-holder")?.currentHp)
		assertEquals(1, resolved.participant("gluttony-holder")?.statStage(BattleStat.ATTACK))
		assertEquals(null, resolved.participant("gluttony-holder")?.itemId)
	}
}
