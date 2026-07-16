package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleFieldDamageAuraAbilityTests {
	@Test
	fun `aura break reverses an active dark aura for every battler`() {
		val skill = damagingSkill(skillId = 1085, power = 40, elementId = 17)
		val resolved = BattleEngine().resolveTurn(
			BattleEngine().start(
				doubleInitialState(
					firstA = participant("attacker", 100, skill = skill),
					firstB = participant(
						"aura-holder",
						80,
						abilityEffects = listOf(BattleAbilityEffect.FieldElementSkillDamageAura(17L, 4.0 / 3.0, 3.0 / 4.0)),
					),
					secondA = participant("target", 70),
					secondB = participant(
						"aura-break-holder",
						60,
						abilityEffects = listOf(BattleAbilityEffect.FieldDamageAuraReversal()),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(14, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
	}
}
