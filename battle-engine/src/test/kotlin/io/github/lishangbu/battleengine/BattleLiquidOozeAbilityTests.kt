package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertTrue

class BattleLiquidOozeAbilityTests {
	@Test
	fun `liquid ooze turns drain healing into damage`() {
		val skill = damagingSkill(
			skillId = 961,
			power = 40,
			hpEffects = listOf(BattleSkillHpEffect.DrainDamage(1, 2)),
		)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, currentHp = 50, skill = skill),
					second = participant(
						"holder",
						50,
						abilityEffects = listOf(BattleAbilityEffect.DrainHealingReversal()),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertTrue(requireNotNull(resolved.participant("attacker")).currentHp < 50)
	}
}
